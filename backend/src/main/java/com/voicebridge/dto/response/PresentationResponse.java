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
public class PresentationResponse {
    private Long id;
    private Long meetingId;
    private String fileName;
    private String fileType;
    private String storagePath;
    private Integer totalSlides;
    private Integer currentSlide;
    private Instant uploadedAt;
}
