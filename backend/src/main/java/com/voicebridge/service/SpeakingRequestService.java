package com.voicebridge.service;

import com.voicebridge.dto.request.ReorderQueueRequest;
import com.voicebridge.dto.response.SpeakingRequestResponse;

import java.util.List;

public interface SpeakingRequestService {

    SpeakingRequestResponse raiseHand(String sessionToken);

    SpeakingRequestResponse getMyRequest(String sessionToken);

    SpeakingRequestResponse startSpeaking(String sessionToken);

    SpeakingRequestResponse stopSpeaking(String sessionToken);

    List<SpeakingRequestResponse> listQueue(Long organizerId, Long meetingId);

    SpeakingRequestResponse approve(Long organizerId, Long meetingId, Long requestId);

    SpeakingRequestResponse reject(Long organizerId, Long meetingId, Long requestId);

    SpeakingRequestResponse endSpeaker(Long organizerId, Long meetingId, Long requestId);

    List<SpeakingRequestResponse> reorderQueue(Long organizerId, Long meetingId, ReorderQueueRequest request);
}
