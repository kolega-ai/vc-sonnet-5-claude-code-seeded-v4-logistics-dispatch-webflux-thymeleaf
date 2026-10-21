package com.fielddispatch.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Backing object for the message composer on the job detail page. The thread
 * renders each entry with the sender's role badge and links it to the sender's
 * profile, so the composer submits both alongside the text.
 */
@Data
public class JobMessageForm {

    @NotBlank
    private String body;

    private String senderRole;

    private Long senderId;
}
