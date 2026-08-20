package com.voicebridge.service.impl;

import com.voicebridge.dto.request.JoinMeetingRequest;
import com.voicebridge.dto.response.ParticipantResponse;
import com.voicebridge.entity.Meeting;
import com.voicebridge.entity.Participant;
import com.voicebridge.entity.enums.MeetingStatus;
import com.voicebridge.entity.enums.ParticipantStatus;
import com.voicebridge.exception.BusinessRuleViolationException;
import com.voicebridge.exception.DuplicateResourceException;
import com.voicebridge.exception.ResourceNotFoundException;
import com.voicebridge.mapper.ParticipantMapper;
import com.voicebridge.repository.MeetingRepository;
import com.voicebridge.repository.ParticipantRepository;
import com.voicebridge.service.ParticipantService;
import com.voicebridge.utils.TokenGenerator;
import com.voicebridge.websocket.MeetingEventPublisher;
import com.voicebridge.websocket.MeetingEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipantServiceImpl implements ParticipantService {

    private final ParticipantRepository participantRepository;
    private final MeetingRepository meetingRepository;
    private final ParticipantMapper participantMapper;
    private final MeetingEventPublisher eventPublisher;

    @Override
    @Transactional
    public ParticipantResponse join(String meetingCode, JoinMeetingRequest request) {
        Meeting meeting = meetingRepository.findByMeetingCode(meetingCode)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        if (meeting.getStatus() == MeetingStatus.CLOSED) {
            throw new BusinessRuleViolationException("This meeting has been closed by the organizer");
        }

        String trimmedName = request.getName().trim();
        if (participantRepository.existsByMeetingIdAndNameIgnoreCase(meeting.getId(), trimmedName)) {
            throw new DuplicateResourceException("This name is already taken in this meeting. Please choose another.");
        }

        Participant participant = Participant.builder()
                .meeting(meeting)
                .name(trimmedName)
                .status(ParticipantStatus.CONNECTED)
                .sessionToken(TokenGenerator.generate())
                .build();

        participant = participantRepository.save(participant);
        log.info("Participant joined: meetingCode={}, participantId={}, name={}",
                meetingCode, participant.getId(), participant.getName());

        ParticipantResponse response = participantMapper.toResponse(participant);
        eventPublisher.publish(meetingCode, MeetingEventType.PARTICIPANT_JOINED, response);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ParticipantResponse getBySessionToken(String sessionToken) {
        Participant participant = participantRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new ResourceNotFoundException("Participant session not found"));
        return participantMapper.toResponse(participant);
    }

    @Override
    @Transactional
    public void leave(String sessionToken) {
        Participant participant = participantRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new ResourceNotFoundException("Participant session not found"));

        participant.setActive(false);
        participant.setStatus(ParticipantStatus.DISCONNECTED);
        participant.setLeftAt(Instant.now());
        participantRepository.save(participant);
        log.info("Participant left: meetingCode={}, participantId={}",
                participant.getMeeting().getMeetingCode(), participant.getId());

        eventPublisher.publish(participant.getMeeting().getMeetingCode(), MeetingEventType.PARTICIPANT_LEFT,
                participantMapper.toResponse(participant));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipantResponse> listParticipants(Long organizerId, Long meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        if (!meeting.getOrganizer().getId().equals(organizerId)) {
            throw new ResourceNotFoundException("Meeting not found");
        }

        return participantRepository.findByMeetingIdOrderByJoinedAtAsc(meetingId).stream()
                .map(participantMapper::toResponse)
                .toList();
    }
}
