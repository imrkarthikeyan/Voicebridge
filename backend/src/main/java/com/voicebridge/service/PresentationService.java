package com.voicebridge.service;

import com.voicebridge.dto.request.ChangeSlideRequest;
import com.voicebridge.dto.response.PresentationResponse;
import com.voicebridge.dto.response.PresentationSessionResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PresentationService {

    PresentationResponse uploadPresentation(Long organizerId, Long meetingId, MultipartFile file);

    List<PresentationResponse> listPresentations(Long meetingId);

    PresentationResponse getPresentation(Long presentationId);

    void deletePresentation(Long organizerId, Long presentationId);

    PresentationSessionResponse startPresentation(Long organizerId, Long presentationId);

    PresentationSessionResponse stopPresentation(Long organizerId, Long presentationId);

    PresentationSessionResponse changeSlide(Long organizerId, Long presentationId, ChangeSlideRequest request);

    PresentationSessionResponse getSession(Long presentationId);

    PresentationSessionResponse getMeetingSession(Long meetingId);

    byte[] getSlideImage(Long presentationId, Integer slideNumber);
}
