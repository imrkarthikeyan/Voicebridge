package com.voicebridge.dto.response;

import com.voicebridge.entity.enums.ParticipantStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantResponse {

    private Long id;
    private String name;
    private String meetingCode;
    private String sessionToken;
    private String connectionId;
    private ParticipantStatus status;
    private boolean active;
    private Instant joinedAt;
    private Instant leftAt;
    private Instant lastSeenAt;
}
