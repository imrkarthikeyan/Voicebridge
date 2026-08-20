package com.voicebridge.service;

import com.voicebridge.dto.request.CreateMeetingRequest;
import com.voicebridge.dto.response.MeetingJoinInfoResponse;
import com.voicebridge.dto.response.MeetingResponse;

import java.util.List;

public interface MeetingService {

    MeetingResponse createMeeting(Long organizerId, CreateMeetingRequest request);

    List<MeetingResponse> listMeetingsForOrganizer(Long organizerId);

    MeetingResponse getMeeting(Long organizerId, String meetingIdentifier);

    MeetingResponse closeMeeting(Long organizerId, String meetingIdentifier);

    byte[] generateQrCode(String meetingCode);

    MeetingJoinInfoResponse getJoinInfo(String meetingCode);
}
