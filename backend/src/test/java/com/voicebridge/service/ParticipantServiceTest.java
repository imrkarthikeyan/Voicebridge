package com.voicebridge.service;

import com.voicebridge.dto.request.JoinMeetingRequest;
import com.voicebridge.dto.response.ParticipantResponse;
import com.voicebridge.entity.Meeting;
import com.voicebridge.entity.enums.MeetingStatus;
import com.voicebridge.entity.Participant;
import com.voicebridge.entity.enums.ParticipantStatus;
import com.voicebridge.exception.BusinessRuleViolationException;
import com.voicebridge.exception.DuplicateResourceException;
import com.voicebridge.exception.ResourceNotFoundException;
import com.voicebridge.mapper.ParticipantMapper;
import com.voicebridge.repository.MeetingRepository;
import com.voicebridge.repository.ParticipantRepository;
import com.voicebridge.service.impl.ParticipantServiceImpl;
import com.voicebridge.websocket.MeetingEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ParticipantServiceTest {

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private ParticipantMapper participantMapper;

    @Mock
    private MeetingEventPublisher eventPublisher;

    @InjectMocks
    private ParticipantServiceImpl participantService;

    private Meeting meeting;
    private Participant participant;
    private ParticipantResponse mockParticipantResponse;

    @BeforeEach
    void setUp() {
        meeting = Meeting.builder()
                .id(1L)
                .meetingCode("M12345")
                .status(MeetingStatus.ACTIVE)
                .build();

        participant = Participant.builder()
                .id(10L)
                .name("Alice")
                .meeting(meeting)
                .sessionToken("valid-session-token")
                .status(ParticipantStatus.CONNECTED)
                .active(true)
                .joinedAt(Instant.now())
                .lastSeenAt(Instant.now())
                .build();

        mockParticipantResponse = ParticipantResponse.builder()
                .id(10L)
                .name("Alice")
                .sessionToken("valid-session-token")
                .status(ParticipantStatus.CONNECTED)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Should successfully join an active meeting with valid name")
    void join_Success() {
        JoinMeetingRequest request = new JoinMeetingRequest();
        request.setName("Alice");

        when(meetingRepository.findByMeetingCode("M12345")).thenReturn(Optional.of(meeting));
        when(participantRepository.existsByMeetingIdAndNameIgnoreCase(1L, "Alice")).thenReturn(false);
        when(participantRepository.save(any(Participant.class))).thenReturn(participant);
        when(participantMapper.toResponse(any(Participant.class))).thenReturn(mockParticipantResponse);

        ParticipantResponse response = participantService.join("M12345", request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Alice");
        assertThat(response.getSessionToken()).isEqualTo("valid-session-token");
        verify(eventPublisher).publish(eq("M12345"), any(), any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when joining nonexistent meeting")
    void join_NonexistentMeeting() {
        JoinMeetingRequest request = new JoinMeetingRequest();
        request.setName("Bob");

        when(meetingRepository.findByMeetingCode("INVALID")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> participantService.join("INVALID", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Meeting not found");
    }

    @Test
    @DisplayName("Should throw BusinessRuleViolationException when joining closed meeting")
    void join_ClosedMeeting() {
        meeting.setStatus(MeetingStatus.CLOSED);
        JoinMeetingRequest request = new JoinMeetingRequest();
        request.setName("Bob");

        when(meetingRepository.findByMeetingCode("M12345")).thenReturn(Optional.of(meeting));

        assertThatThrownBy(() -> participantService.join("M12345", request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("This meeting has been closed by the organizer");
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when joining with duplicate active name")
    void join_DuplicateActiveName() {
        JoinMeetingRequest request = new JoinMeetingRequest();
        request.setName("Alice");

        when(meetingRepository.findByMeetingCode("M12345")).thenReturn(Optional.of(meeting));
        when(participantRepository.existsByMeetingIdAndNameIgnoreCase(1L, "Alice")).thenReturn(true);

        assertThatThrownBy(() -> participantService.join("M12345", request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("This name is already taken in this meeting");
    }

    @Test
    @DisplayName("Should set participant inactive when participant leaves meeting")
    void leave_Success() {
        when(participantRepository.findBySessionToken("valid-session-token")).thenReturn(Optional.of(participant));
        when(participantRepository.save(any(Participant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        participantService.leave("valid-session-token");

        assertThat(participant.isActive()).isFalse();
        assertThat(participant.getStatus()).isEqualTo(ParticipantStatus.DISCONNECTED);
        assertThat(participant.getLeftAt()).isNotNull();
        verify(eventPublisher).publish(eq("M12345"), any(), any());
    }

    @Test
    @DisplayName("Should retrieve participant by valid session token")
    void getBySessionToken_Success() {
        when(participantRepository.findBySessionToken("valid-session-token")).thenReturn(Optional.of(participant));
        when(participantMapper.toResponse(any(Participant.class))).thenReturn(mockParticipantResponse);

        ParticipantResponse response = participantService.getBySessionToken("valid-session-token");

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getName()).isEqualTo("Alice");
    }
}
