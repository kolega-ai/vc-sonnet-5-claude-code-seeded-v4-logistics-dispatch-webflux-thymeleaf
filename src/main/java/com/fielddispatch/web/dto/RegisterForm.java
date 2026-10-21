package com.fielddispatch.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Explicit allow-list DTO for self-registration. Notably absent: role,
 * enabled, regionId (server-assigned) - a client can never bind those fields.
 */
@Data
public class RegisterForm {
    @NotBlank @Email
    private String email;
    @NotBlank @Size(min = 8, max = 100)
    private String password;
    @NotBlank
    private String fullName;
    private String phone;
    private Long regionId;
}
