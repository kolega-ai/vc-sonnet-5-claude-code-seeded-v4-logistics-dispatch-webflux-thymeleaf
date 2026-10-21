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
@Table("proofs")
public class Proof {
    @Id
    private Long id;
    private Long jobId;
    private Long uploadedBy;
    private String category;
    private String visibility;
    private String storedFilename;
    private String originalFilename;
    private String contentType;
    private long sizeBytes;
    private OffsetDateTime createdAt;

    public boolean isCustomerVisible() {
        return "CUSTOMER_VISIBLE".equals(visibility);
    }
}
