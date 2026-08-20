package com.voicebridge.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RaiseHandRequest {

    @NotBlank(message = "Participant session token is required")
    private String participantSessionToken;
}
