package com.voicebridge.dto.response;

import com.voicebridge.entity.enums.MeetingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingStatusResponse {
    private Long id;
    private String meetingCode;
    private MeetingStatus status;
    private Instant startedAt;
    private Instant endedAt;
}
