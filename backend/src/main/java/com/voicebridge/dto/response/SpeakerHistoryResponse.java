package com.voicebridge.dto.response;

import com.voicebridge.entity.enums.SpeakerEndReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpeakerHistoryResponse {
    private Long id;
    private Long meetingId;
    private Long participantId;
    private Instant startedAt;
    private Instant endedAt;
    private Long durationSeconds;
    private SpeakerEndReason endReason;
}
