package com.voicebridge.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizerSummaryResponse {

    private Long id;
    private String name;
    private String email;
    private Instant createdAt;
}
