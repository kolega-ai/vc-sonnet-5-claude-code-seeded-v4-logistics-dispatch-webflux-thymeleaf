package com.fielddispatch.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Job workflow states with an explicit allowed-transition edge list. Every
 * status is reachable through a stated action (see JobService).
 */
public enum JobStatus {
    unassigned,
    assigned,
    en_route,
    arrived,
    in_progress,
    completed,
    cancelled,
    failed;

    private static final Map<JobStatus, Set<JobStatus>> TRANSITIONS = new EnumMap<>(JobStatus.class);

    static {
        TRANSITIONS.put(unassigned, EnumSet.of(assigned, cancelled));
        TRANSITIONS.put(assigned, EnumSet.of(en_route, unassigned, cancelled));
        TRANSITIONS.put(en_route, EnumSet.of(arrived, cancelled, failed));
        TRANSITIONS.put(arrived, EnumSet.of(in_progress, failed, cancelled));
        TRANSITIONS.put(in_progress, EnumSet.of(completed, failed, cancelled));
        TRANSITIONS.put(completed, EnumSet.noneOf(JobStatus.class));
        TRANSITIONS.put(cancelled, EnumSet.of(unassigned));
        TRANSITIONS.put(failed, EnumSet.of(unassigned));
    }

    public boolean canTransitionTo(JobStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }
}
