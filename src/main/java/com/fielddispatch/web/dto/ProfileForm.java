package com.fielddispatch.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProfileForm {
    @NotBlank
    private String fullName;
    private String phone;
}
