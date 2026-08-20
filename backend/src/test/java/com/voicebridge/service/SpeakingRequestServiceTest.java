package com.voicebridge.service;

import com.voicebridge.dto.request.ReorderQueueRequest;
import com.voicebridge.dto.response.SpeakingRequestResponse;
import com.voicebridge.entity.Meeting;
import com.voicebridge.entity.enums.MeetingStatus;
import com.voicebridge.entity.Organizer;
import com.voicebridge.entity.Participant;
import com.voicebridge.entity.SpeakingRequest;
import com.voicebridge.entity.enums.SpeakingRequestStatus;
import com.voicebridge.exception.BusinessRuleViolationException;
import com.voicebridge.mapper.SpeakingRequestMapper;
import com.voicebridge.repository.MeetingRepository;
import com.voicebridge.repository.ParticipantRepository;
import com.voicebridge.repository.SpeakerHistoryRepository;
import com.voicebridge.repository.SpeakingRequestRepository;
import com.voicebridge.service.impl.SpeakingRequestServiceImpl;
import com.voicebridge.websocket.MeetingEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SpeakingRequestServiceTest {

    @Mock
    private SpeakingRequestRepository speakingRequestRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private SpeakerHistoryRepository speakerHistoryRepository;

    @Mock
    private SpeakingRequestMapper speakingRequestMapper;

    @Mock
    private MeetingEventPublisher eventPublisher;

    @InjectMocks
    private SpeakingRequestServiceImpl speakingRequestService;

    private Organizer organizer;
    private Meeting meeting;
    private Participant participant1;
    private Participant participant2;
    private SpeakingRequest request1;
    private SpeakingRequest request2;
    private SpeakingRequestResponse mockResponse1;

    @BeforeEach
    void setUp() {
        organizer = Organizer.builder().id(10L).email("organizer@test.com").build();
        meeting = Meeting.builder().id(100L).meetingCode("M100").status(MeetingStatus.ACTIVE).organizer(organizer).build();

        participant1 = Participant.builder().id(1L).name("P1").meeting(meeting).sessionToken("token-1").active(true).build();
        participant2 = Participant.builder().id(2L).name("P2").meeting(meeting).sessionToken("token-2").active(true).build();

        request1 = SpeakingRequest.builder()
                .id(501L)
                .meeting(meeting)
                .participant(participant1)
                .status(SpeakingRequestStatus.WAITING)
                .queuePosition(1)
                .requestedAt(Instant.now())
                .build();

        request2 = SpeakingRequest.builder()
                .id(502L)
                .meeting(meeting)
                .participant(participant2)
                .status(SpeakingRequestStatus.WAITING)
                .queuePosition(2)
                .requestedAt(Instant.now())
                .build();

        mockResponse1 = SpeakingRequestResponse.builder()
                .id(501L)
                .status(SpeakingRequestStatus.WAITING)
                .build();
    }

    @Test
    @DisplayName("Should successfully raise hand for waiting queue position")
    void raiseHand_Success() {
        when(participantRepository.findBySessionToken("token-1")).thenReturn(Optional.of(participant1));
        when(speakingRequestRepository.existsByParticipantIdAndStatusIn(eq(1L), any())).thenReturn(false);
        when(speakingRequestRepository.findByMeetingIdOrderByQueuePositionAsc(100L)).thenReturn(List.of());
        when(speakingRequestRepository.save(any(SpeakingRequest.class))).thenReturn(request1);
        when(speakingRequestMapper.toResponse(any())).thenReturn(mockResponse1);

        SpeakingRequestResponse response = speakingRequestService.raiseHand("token-1");

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(501L);
        verify(eventPublisher).publish(eq("M100"), any(), any());
    }

    @Test
    @DisplayName("Should throw BusinessRuleViolationException when participant raises hand again")
    void raiseHand_DuplicateHandRaise() {
        when(participantRepository.findBySessionToken("token-1")).thenReturn(Optional.of(participant1));
        when(speakingRequestRepository.existsByParticipantIdAndStatusIn(eq(1L), any())).thenReturn(true);

        assertThatThrownBy(() -> speakingRequestService.raiseHand("token-1"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("You have already raised your hand");
    }

    @Test
    @DisplayName("Should approve waiting request when no active speaker exists")
    void approve_Success() {
        when(speakingRequestRepository.findById(501L)).thenReturn(Optional.of(request1));
        when(speakingRequestRepository.findFirstByMeetingIdAndStatusIn(eq(100L), any()))
                .thenReturn(Optional.empty());
        when(speakingRequestRepository.save(any(SpeakingRequest.class))).thenAnswer(i -> i.getArgument(0));
        when(speakingRequestMapper.toResponse(any())).thenReturn(mockResponse1);

        SpeakingRequestResponse response = speakingRequestService.approve(10L, 100L, 501L);

        assertThat(response).isNotNull();
        verify(eventPublisher).publish(eq("M100"), any(), any());
    }

    @Test
    @DisplayName("Should throw BusinessRuleViolationException when another participant is already approved/speaking")
    void approve_FloorOccupied() {
        when(speakingRequestRepository.findById(502L)).thenReturn(Optional.of(request2));
        when(speakingRequestRepository.findFirstByMeetingIdAndStatusIn(eq(100L), any()))
                .thenReturn(Optional.of(request1));

        assertThatThrownBy(() -> speakingRequestService.approve(10L, 100L, 502L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Another participant is already approved or speaking");
    }

    @Test
    @DisplayName("Should reject waiting speaking request")
    void reject_Success() {
        when(speakingRequestRepository.findById(501L)).thenReturn(Optional.of(request1));
        when(speakingRequestRepository.save(any(SpeakingRequest.class))).thenAnswer(i -> i.getArgument(0));
        when(speakingRequestMapper.toResponse(any())).thenReturn(mockResponse1);

        SpeakingRequestResponse response = speakingRequestService.reject(10L, 100L, 501L);

        assertThat(response).isNotNull();
        verify(eventPublisher).publish(eq("M100"), any(), any());
    }

    @Test
    @DisplayName("Should end active speaker by organizer and record history")
    void endSpeaker_Success() {
        request1.setStatus(SpeakingRequestStatus.SPEAKING);
        request1.setStartedAt(Instant.now().minusSeconds(30));

        when(speakingRequestRepository.findById(501L)).thenReturn(Optional.of(request1));
        when(speakingRequestRepository.save(any(SpeakingRequest.class))).thenAnswer(i -> i.getArgument(0));
        when(speakingRequestMapper.toResponse(any())).thenReturn(mockResponse1);

        SpeakingRequestResponse response = speakingRequestService.endSpeaker(10L, 100L, 501L);

        assertThat(response).isNotNull();
        verify(speakerHistoryRepository).save(any());
        verify(eventPublisher).publish(eq("M100"), any(), any());
    }

    @Test
    @DisplayName("Should reorder queue according to provided request IDs")
    void reorderQueue_Success() {
        ReorderQueueRequest request = new ReorderQueueRequest();
        request.setOrderedRequestIds(List.of(501L, 502L));

        when(meetingRepository.findById(100L)).thenReturn(Optional.of(meeting));
        when(speakingRequestRepository.findByMeetingIdAndStatusOrderByQueuePositionAsc(100L, SpeakingRequestStatus.WAITING))
                .thenReturn(List.of(request1, request2));
        when(speakingRequestRepository.findByMeetingIdOrderByQueuePositionAsc(100L))
                .thenReturn(List.of(request1, request2));

        List<SpeakingRequestResponse> queue = speakingRequestService.reorderQueue(10L, 100L, request);

        assertThat(queue).hasSize(2);
        verify(speakingRequestRepository).saveAll(any());
        verify(eventPublisher).publish(eq("M100"), any(), any());
    }
}
