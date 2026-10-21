package com.fielddispatch.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordChangeForm {
    @NotBlank
    private String currentPassword;
    @NotBlank @Size(min = 8, max = 100)
    private String newPassword;
}
