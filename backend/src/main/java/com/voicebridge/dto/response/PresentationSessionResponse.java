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
public class PresentationSessionResponse {
    private Long id;
    private Long meetingId;
    private Long presentationId;
    private Integer currentSlide;
    private boolean presenting;
    private Instant updatedAt;
}
