package com.fielddispatch.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class JobCreateForm {
    @NotNull
    private Long customerId;
    @NotNull
    private Long locationId;
    @NotNull
    private Long serviceTypeId;
    @NotNull
    private Long regionId;
    private String priority = "normal";
    @NotNull
    private LocalDateTime scheduledStart;
    @NotNull
    private LocalDateTime scheduledEnd;
    private String description;
}
