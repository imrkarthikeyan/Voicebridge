package com.voicebridge.service;

import com.voicebridge.dto.request.JoinMeetingRequest;
import com.voicebridge.dto.response.ParticipantResponse;

import java.util.List;

public interface ParticipantService {

    ParticipantResponse join(String meetingCode, JoinMeetingRequest request);

    ParticipantResponse getBySessionToken(String sessionToken);

    void leave(String sessionToken);

    List<ParticipantResponse> listParticipants(Long organizerId, Long meetingId);
}
