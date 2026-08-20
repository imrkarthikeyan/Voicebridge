package com.voicebridge.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JoinMeetingRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 80, message = "Name must be between 1 and 80 characters")
    @Pattern(regexp = "^[\\p{L}\\p{N} ._'-]+$", message = "Name contains unsupported characters")
    private String name;
}
