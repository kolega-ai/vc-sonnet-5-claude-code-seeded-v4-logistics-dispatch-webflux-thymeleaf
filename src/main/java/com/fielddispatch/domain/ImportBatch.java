package com.fielddispatch.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("import_batches")
public class ImportBatch {
    @Id
    private Long id;
    private Long importedBy;
    private String entityType;
    private int rowCount;
    private int successCount;
    private int errorCount;
    private String errorDetail;
    private OffsetDateTime createdAt;
}
