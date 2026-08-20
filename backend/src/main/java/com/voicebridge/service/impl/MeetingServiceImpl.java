package com.voicebridge.service.impl;

import com.voicebridge.dto.request.CreateMeetingRequest;
import com.voicebridge.dto.response.MeetingJoinInfoResponse;
import com.voicebridge.dto.response.MeetingResponse;
import com.voicebridge.entity.Meeting;
import com.voicebridge.entity.Organizer;
import com.voicebridge.entity.enums.MeetingStatus;
import com.voicebridge.exception.BusinessRuleViolationException;
import com.voicebridge.exception.ResourceNotFoundException;
import com.voicebridge.mapper.MeetingMapper;
import com.voicebridge.repository.MeetingRepository;
import com.voicebridge.repository.OrganizerRepository;
import com.voicebridge.service.MeetingService;
import com.voicebridge.utils.MeetingCodeGenerator;
import com.voicebridge.utils.QrCodeGenerator;
import com.voicebridge.utils.TokenGenerator;
import com.voicebridge.websocket.MeetingEventPublisher;
import com.voicebridge.websocket.MeetingEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingServiceImpl implements MeetingService {

    private static final int MAX_CODE_GENERATION_ATTEMPTS = 10;

    private final MeetingRepository meetingRepository;
    private final OrganizerRepository organizerRepository;
    private final MeetingMapper meetingMapper;
    private final MeetingEventPublisher eventPublisher;

    @Value("${app.qr.base-url}")
    private String qrBaseUrl;

    @Override
    @Transactional
    public MeetingResponse createMeeting(Long organizerId, CreateMeetingRequest request) {
        Organizer organizerRef = organizerRepository.getReferenceById(organizerId);

        Meeting meeting = Meeting.builder()
                .meetingCode(generateUniqueMeetingCode())
                .title(request.getTitle().trim())
                .description(request.getDescription() == null ? null : request.getDescription().trim())
                .organizer(organizerRef)
                .status(MeetingStatus.ACTIVE)
                .qrToken(generateUniqueQrToken())
                .build();

        meeting = meetingRepository.save(meeting);
        log.info("Meeting created: id={}, code={}, organizerId={}", meeting.getId(), meeting.getMeetingCode(), organizerId);

        return toResponseWithLinks(meeting);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeetingResponse> listMeetingsForOrganizer(Long organizerId) {
        return meetingRepository.findByOrganizerIdOrderByCreatedAtDesc(organizerId).stream()
                .map(this::toResponseWithLinks)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MeetingResponse getMeeting(Long organizerId, String meetingIdentifier) {
        Meeting meeting = getOwnedMeeting(organizerId, meetingIdentifier);
        return toResponseWithLinks(meeting);
    }

    @Override
    @Transactional
    public MeetingResponse closeMeeting(Long organizerId, String meetingIdentifier) {
        Meeting meeting = getOwnedMeeting(organizerId, meetingIdentifier);

        if (meeting.getStatus() == MeetingStatus.CLOSED) {
            throw new BusinessRuleViolationException("Meeting is already closed");
        }

        meeting.setStatus(MeetingStatus.CLOSED);
        meeting.setEndedAt(Instant.now());
        meeting = meetingRepository.save(meeting);
        log.info("Meeting closed: id={}, code={}", meeting.getId(), meeting.getMeetingCode());

        MeetingResponse response = toResponseWithLinks(meeting);
        eventPublisher.publish(meeting.getMeetingCode(), MeetingEventType.MEETING_CLOSED, response);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateQrCode(String meetingCode) {
        Meeting meeting = meetingRepository.findByMeetingCode(meetingCode)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
        return QrCodeGenerator.generatePng(buildJoinUrl(meeting.getMeetingCode()));
    }

    @Override
    @Transactional(readOnly = true)
    public MeetingJoinInfoResponse getJoinInfo(String meetingCode) {
        Meeting meeting = meetingRepository.findByMeetingCode(meetingCode)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        return MeetingJoinInfoResponse.builder()
                .meetingCode(meeting.getMeetingCode())
                .title(meeting.getTitle())
                .status(meeting.getStatus())
                .build();
    }

    private Meeting getOwnedMeeting(Long organizerId, String meetingIdentifier) {
        Meeting meeting;
        if (meetingIdentifier.matches("^\\d+$")) {
            Long meetingId = Long.parseLong(meetingIdentifier);
            meeting = meetingRepository.findById(meetingId)
                    .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
        } else {
            meeting = meetingRepository.findByMeetingCode(meetingIdentifier)
                    .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
        }

        if (!meeting.getOrganizer().getId().equals(organizerId)) {
            throw new ResourceNotFoundException("Meeting not found");
        }
        return meeting;
    }

    private MeetingResponse toResponseWithLinks(Meeting meeting) {
        MeetingResponse response = meetingMapper.toResponse(meeting);
        response.setJoinUrl(buildJoinUrl(meeting.getMeetingCode()));
        response.setQrCodeUrl("/api/meetings/" + meeting.getMeetingCode() + "/qr");
        return response;
    }

    private String buildJoinUrl(String meetingCode) {
        return qrBaseUrl + "/join/" + meetingCode;
    }

    private String generateUniqueQrToken() {
        for (int attempt = 0; attempt < MAX_CODE_GENERATION_ATTEMPTS; attempt++) {
            String candidate = TokenGenerator.generate();
            if (!meetingRepository.existsByQrToken(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to generate a unique QR token");
    }

    private String generateUniqueMeetingCode() {
        for (int attempt = 0; attempt < MAX_CODE_GENERATION_ATTEMPTS; attempt++) {
            String candidate = MeetingCodeGenerator.generate();
            if (!meetingRepository.existsByMeetingCode(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to generate a unique meeting code");
    }
}
