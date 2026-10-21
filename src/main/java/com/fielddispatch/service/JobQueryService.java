package com.fielddispatch.service;

import com.fielddispatch.domain.Job;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * PRECISION BASELINE #4: the dispatch board's sort column is taken from a
 * fixed whitelist map, never concatenated from client input. Every value
 * bound into the SQL (region ids, status, priority, worker id, date range) is
 * a bound parameter; the ONLY interpolated fragment is the ORDER BY column
 * name, and it can only ever be one of the whitelisted values below.
 */
@Service
public class JobQueryService {

    private static final Map<String, String> SORT_WHITELIST = Map.of(
            "scheduled_start", "scheduled_start",
            "priority", "priority",
            "status", "status",
            "created_at", "created_at",
            "region_id", "region_id"
    );
    private static final Set<String> DIRECTION_WHITELIST = Set.of("ASC", "DESC");

    private final DatabaseClient databaseClient;

    public JobQueryService(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    public Flux<Job> search(List<Long> allowedRegionIds, String status, String priority, Long workerId,
                             OffsetDateTime from, OffsetDateTime to, String sortKey, String direction) {
        String sortColumn = SORT_WHITELIST.getOrDefault(sortKey, "scheduled_start");
        String sortDirection = DIRECTION_WHITELIST.contains(direction) ? direction : "ASC";

        StringBuilder sql = new StringBuilder("SELECT * FROM jobs WHERE region_id = ANY(:regionIds)");
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = :status");
        }
        if (priority != null && !priority.isBlank()) {
            sql.append(" AND priority = :priority");
        }
        if (workerId != null) {
            sql.append(" AND assigned_worker_id = :workerId");
        }
        if (from != null) {
            sql.append(" AND scheduled_start >= :from");
        }
        if (to != null) {
            sql.append(" AND scheduled_start <= :to");
        }
        // sortColumn/sortDirection are validated against fixed whitelists above - never raw user input.
        sql.append(" ORDER BY ").append(sortColumn).append(' ').append(sortDirection).append(" LIMIT 500");

        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql.toString())
                .bind("regionIds", allowedRegionIds.toArray(new Long[0]));
        if (status != null && !status.isBlank()) {
            spec = spec.bind("status", status);
        }
        if (priority != null && !priority.isBlank()) {
            spec = spec.bind("priority", priority);
        }
        if (workerId != null) {
            spec = spec.bind("workerId", workerId);
        }
        if (from != null) {
            spec = spec.bind("from", from);
        }
        if (to != null) {
            spec = spec.bind("to", to);
        }

        return spec.map((row, meta) -> Job.builder()
                        .id(row.get("id", Long.class))
                        .customerId(row.get("customer_id", Long.class))
                        .locationId(row.get("location_id", Long.class))
                        .serviceTypeId(row.get("service_type_id", Long.class))
                        .regionId(row.get("region_id", Long.class))
                        .assignedWorkerId(row.get("assigned_worker_id", Long.class))
                        .status(row.get("status", String.class))
                        .priority(row.get("priority", String.class))
                        .scheduledStart(row.get("scheduled_start", OffsetDateTime.class))
                        .scheduledEnd(row.get("scheduled_end", OffsetDateTime.class))
                        .description(row.get("description", String.class))
                        .rescheduleCount(row.get("reschedule_count", Integer.class) != null ? row.get("reschedule_count", Integer.class) : 0)
                        .createdAt(row.get("created_at", OffsetDateTime.class))
                        .updatedAt(row.get("updated_at", OffsetDateTime.class))
                        .build())
                .all();
    }
}
