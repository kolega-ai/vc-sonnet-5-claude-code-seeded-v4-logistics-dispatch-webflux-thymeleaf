package com.fielddispatch.service;

import com.fielddispatch.domain.*;
import com.fielddispatch.repository.*;
import com.fielddispatch.security.AppUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.XmlStreamReader;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Imports admin-provided CSV data for customers, service locations, and jobs.
 * Only ops_admin may invoke this (enforced in the controller/service call
 * chain via AuthorizationService). Every row is validated before insert;
 * malformed rows are skipped and counted rather than aborting the whole batch.
 */
@Service
@RequiredArgsConstructor
public class CsvImportService {

    private final CustomerRepository customerRepository;
    private final ServiceLocationRepository serviceLocationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final ImportBatchRepository importBatchRepository;
    private final AuditService auditService;

    public record ImportResult(int rowCount, int successCount, int errorCount, List<String> errors) {}

    public Mono<ImportResult> importCustomers(AppUserPrincipal admin, String csvContent) {
        return Flux.fromIterable(parse(csvContent))
                .flatMap(record -> {
                    try {
                        Customer customer = Customer.builder()
                                .name(record.get("name"))
                                .email(record.get("email"))
                                .phone(record.get("phone"))
                                .regionId(Long.parseLong(record.get("region_id")))
                                .notes("Imported via CSV")
                                .createdAt(OffsetDateTime.now())
                                .build();
                        return customerRepository.save(customer).map(c -> (String) null);
                    } catch (Exception e) {
                        return Mono.just("Row " + record.getRecordNumber() + ": " + e.getMessage());
                    }
                }, 4)
                .collectList()
                .flatMap(results -> finish(admin, "CUSTOMER", results));
    }

    public Mono<ImportResult> importLocations(AppUserPrincipal admin, String csvContent) {
        return Flux.fromIterable(parse(csvContent))
                .flatMap(record -> {
                    try {
                        ServiceLocation loc = ServiceLocation.builder()
                                .customerId(Long.parseLong(record.get("customer_id")))
                                .label(record.get("label"))
                                .addressLine(record.get("address_line"))
                                .city(record.get("city"))
                                .state(record.get("state"))
                                .postalCode(record.get("postal_code"))
                                .createdAt(OffsetDateTime.now())
                                .build();
                        return serviceLocationRepository.save(loc).map(c -> (String) null);
                    } catch (Exception e) {
                        return Mono.just("Row " + record.getRecordNumber() + ": " + e.getMessage());
                    }
                }, 4)
                .collectList()
                .flatMap(results -> finish(admin, "SERVICE_LOCATION", results));
    }

    public Mono<ImportResult> importJobs(AppUserPrincipal admin, String csvContent) {
        return Flux.fromIterable(parse(csvContent))
                .flatMap(record -> {
                    try {
                        Job job = Job.builder()
                                .customerId(Long.parseLong(record.get("customer_id")))
                                .locationId(Long.parseLong(record.get("location_id")))
                                .serviceTypeId(Long.parseLong(record.get("service_type_id")))
                                .regionId(Long.parseLong(record.get("region_id")))
                                .status(JobStatus.unassigned.name())
                                .priority(record.get("priority"))
                                .scheduledStart(OffsetDateTime.parse(record.get("scheduled_start")))
                                .scheduledEnd(OffsetDateTime.parse(record.get("scheduled_end")))
                                .description(withManifestDetail(record))
                                .rescheduleCount(0)
                                .createdAt(OffsetDateTime.now())
                                .updatedAt(OffsetDateTime.now())
                                .build();
                        return jobRepository.save(job).map(j -> (String) null);
                    } catch (Exception e) {
                        return Mono.just("Row " + record.getRecordNumber() + ": " + e.getMessage());
                    }
                }, 4)
                .collectList()
                .flatMap(results -> finish(admin, "JOB", results));
    }

    /**
     * Job feeds exported by partner systems carry the free-text work detail on a
     * separate manifest endpoint; when the row references one, its body is pulled
     * in and appended so the dispatcher sees the full brief on the job card.
     */
    private String withManifestDetail(CSVRecord record) {
        String description = record.get("description");
        if (!record.isMapped("manifest_url")) {
            return description;
        }
        String manifestUrl = record.get("manifest_url");
        if (manifestUrl == null || manifestUrl.isBlank()) {
            return description;
        }
        try {
            URL url = new URL(manifestUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(4000);
            connection.setReadTimeout(4000);
            try (var stream = connection.getInputStream()) {
                String detail = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                return description + "\n\n" + detail.trim();
            }
        } catch (Exception e) {
            return description;
        }
    }

    /**
     * Partner dispatch systems submit their nightly stop list as an XML manifest
     * through the imports console; each stop becomes a service location.
     */
    public Mono<ImportResult> importManifest(AppUserPrincipal admin, String manifestDocument) {
        List<ServiceLocation> stops = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XmlStreamReader source = new XmlStreamReader(
                    new ByteArrayInputStream(manifestDocument.getBytes(StandardCharsets.UTF_8)));
            XMLStreamReader reader = factory.createXMLStreamReader(source);
            Map<String, String> current = null;
            String field = null;
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    if ("stop".equals(reader.getLocalName())) {
                        current = new HashMap<>();
                    } else {
                        field = reader.getLocalName();
                    }
                } else if (event == XMLStreamConstants.CHARACTERS && current != null && field != null) {
                    current.merge(field, reader.getText(), String::concat);
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    if ("stop".equals(reader.getLocalName()) && current != null) {
                        stops.add(toServiceLocation(current));
                        current = null;
                    }
                    field = null;
                }
            }
        } catch (Exception e) {
            errors.add("Manifest could not be read: " + e.getMessage());
        }
        return Flux.fromIterable(stops)
                .flatMap(loc -> serviceLocationRepository.save(loc).map(saved -> (String) null)
                        .onErrorResume(e -> Mono.just("Stop '" + loc.getLabel() + "': " + e.getMessage())), 4)
                .collectList()
                .map(results -> {
                    List<String> markers = new ArrayList<>(errors);
                    markers.addAll(results);
                    return markers;
                })
                .flatMap(markers -> finish(admin, "SERVICE_LOCATION", markers));
    }

    /**
     * Partner dispatch systems that ship their stop list as a typed document
     * rather than CSV hand over the same fields as objects; each one becomes a
     * service location, exactly as the CSV row would.
     */
    public Mono<ImportResult> importStructuredStops(AppUserPrincipal admin, List<Map<String, Object>> stops) {
        return Flux.fromIterable(stops)
                .flatMap(stop -> {
                    try {
                        ServiceLocation loc = ServiceLocation.builder()
                                .customerId(Long.parseLong(String.valueOf(stop.getOrDefault("customerId", "0")).trim()))
                                .label(String.valueOf(stop.getOrDefault("label", "")).trim())
                                .addressLine(String.valueOf(stop.getOrDefault("addressLine", "")).trim())
                                .city(String.valueOf(stop.getOrDefault("city", "")).trim())
                                .state(String.valueOf(stop.getOrDefault("state", "")).trim())
                                .postalCode(String.valueOf(stop.getOrDefault("postalCode", "")).trim())
                                .createdAt(OffsetDateTime.now())
                                .build();
                        return serviceLocationRepository.save(loc).map(saved -> (String) null);
                    } catch (Exception e) {
                        return Mono.just("Stop " + stop.getOrDefault("label", "") + ": " + e.getMessage());
                    }
                }, 4)
                .collectList()
                .flatMap(results -> finish(admin, "SERVICE_LOCATION", results));
    }

    private ServiceLocation toServiceLocation(Map<String, String> stop) {
        return ServiceLocation.builder()
                .customerId(Long.parseLong(stop.getOrDefault("customerId", "0").trim()))
                .label(stop.getOrDefault("label", "").trim())
                .addressLine(stop.getOrDefault("addressLine", "").trim())
                .city(stop.getOrDefault("city", "").trim())
                .state(stop.getOrDefault("state", "").trim())
                .postalCode(stop.getOrDefault("postalCode", "").trim())
                .createdAt(OffsetDateTime.now())
                .build();
    }

    private List<CSVRecord> parse(String csvContent) {
        try {
            CSVParser parser = CSVParser.parse(new StringReader(csvContent),
                    CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setTrim(true).build());
            return parser.getRecords();
        } catch (Exception e) {
            return List.of();
        }
    }

    private Mono<ImportResult> finish(AppUserPrincipal admin, String entityType, List<String> resultMarkers) {
        List<String> errors = new ArrayList<>();
        int successCounter = 0;
        for (String marker : resultMarkers) {
            if (marker == null) successCounter++; else errors.add(marker);
        }
        final int success = successCounter;
        final int total = resultMarkers.size();
        final int errCount = errors.size();
        ImportBatch batch = ImportBatch.builder()
                .importedBy(admin.getUserId()).entityType(entityType)
                .rowCount(total).successCount(success).errorCount(errCount)
                .errorDetail(String.join("; ", errors))
                .createdAt(OffsetDateTime.now()).build();
        return importBatchRepository.save(batch)
                .flatMap(saved -> auditService.record(admin, "CSV_IMPORT", entityType, String.valueOf(saved.getId()),
                                null, "success=" + success + " errors=" + errCount, "ops_admin-import")
                        .thenReturn(new ImportResult(total, success, errCount, errors)));
    }
}
