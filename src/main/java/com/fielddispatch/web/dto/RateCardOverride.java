package com.fielddispatch.web.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Carrier-facing rate card line. Partner desks publish one override per
 * service type in their nightly YAML drop; the operations console applies them
 * to the matching service type.
 */
@Data
public class RateCardOverride {

    private String name;

    private BigDecimal baseRate;
}
