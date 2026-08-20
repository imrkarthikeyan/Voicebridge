package com.voicebridge.dto.response;

import com.voicebridge.entity.enums.SpeakingRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpeakingRequestResponse {

    private Long id;
    private Long participantId;
    private String participantName;
    private SpeakingRequestStatus status;
    private Integer queuePosition;
    private Integer queueOrder;
    private Instant requestedAt;
    private Instant approvedAt;
    private Instant startedAt;
    private Instant rejectedAt;
    private Instant finishedAt;
    private Instant endedAt;
}
