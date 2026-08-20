package com.voicebridge.service;

import com.voicebridge.dto.request.CreateMeetingRequest;
import com.voicebridge.dto.response.MeetingResponse;
import com.voicebridge.entity.Meeting;
import com.voicebridge.entity.enums.MeetingStatus;
import com.voicebridge.entity.Organizer;
import com.voicebridge.exception.BusinessRuleViolationException;
import com.voicebridge.exception.ResourceNotFoundException;
import com.voicebridge.mapper.MeetingMapper;
import com.voicebridge.repository.MeetingRepository;
import com.voicebridge.repository.OrganizerRepository;
import com.voicebridge.service.impl.MeetingServiceImpl;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MeetingServiceTest {

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private OrganizerRepository organizerRepository;

    @Mock
    private MeetingMapper meetingMapper;

    @Mock
    private MeetingEventPublisher eventPublisher;

    @InjectMocks
    private MeetingServiceImpl meetingService;

    private Organizer organizer;
    private Meeting meeting;
    private MeetingResponse mockMeetingResponse;

    @BeforeEach
    void setUp() {
        organizer = Organizer.builder()
                .id(10L)
                .name("Meeting Owner")
                .email("owner@test.com")
                .build();

        meeting = Meeting.builder()
                .id(100L)
                .meetingCode("ABC123")
                .title("Test Meeting Title")
                .status(MeetingStatus.ACTIVE)
                .organizer(organizer)
                .qrToken("test-qr-token")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        mockMeetingResponse = new MeetingResponse();
        mockMeetingResponse.setId(100L);
        mockMeetingResponse.setMeetingCode("ABC123");
        mockMeetingResponse.setStatus(MeetingStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should successfully create a meeting with unique 6-character code")
    void createMeeting_Success() {
        CreateMeetingRequest request = new CreateMeetingRequest();
        request.setTitle("Test Meeting Title");
        request.setDescription("Unit test description");

        when(organizerRepository.getReferenceById(10L)).thenReturn(organizer);
        when(meetingRepository.existsByMeetingCode(anyString())).thenReturn(false);
        when(meetingRepository.save(any(Meeting.class))).thenReturn(meeting);
        when(meetingMapper.toResponse(any(Meeting.class))).thenReturn(mockMeetingResponse);

        MeetingResponse response = meetingService.createMeeting(10L, request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getMeetingCode()).hasSize(6);
        assertThat(response.getStatus()).isEqualTo(MeetingStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should retrieve meeting by valid meeting code")
    void getMeeting_Success() {
        when(meetingRepository.findByMeetingCode("ABC123")).thenReturn(Optional.of(meeting));
        when(meetingMapper.toResponse(any(Meeting.class))).thenReturn(mockMeetingResponse);

        MeetingResponse response = meetingService.getMeeting(10L, "ABC123");

        assertThat(response).isNotNull();
        assertThat(response.getMeetingCode()).isEqualTo("ABC123");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when meeting code does not exist")
    void getMeeting_NotFound() {
        when(meetingRepository.findByMeetingCode("INVALID")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> meetingService.getMeeting(10L, "INVALID"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Meeting not found");
    }

    @Test
    @DisplayName("Should close an active meeting and publish MEETING_CLOSED event")
    void closeMeeting_Success() {
        when(meetingRepository.findByMeetingCode("ABC123")).thenReturn(Optional.of(meeting));
        when(meetingRepository.save(any(Meeting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MeetingResponse closedResponse = new MeetingResponse();
        closedResponse.setId(100L);
        closedResponse.setMeetingCode("ABC123");
        closedResponse.setStatus(MeetingStatus.CLOSED);
        closedResponse.setClosedAt(Instant.now());
        when(meetingMapper.toResponse(any(Meeting.class))).thenReturn(closedResponse);

        MeetingResponse response = meetingService.closeMeeting(10L, "ABC123");

        assertThat(response.getStatus()).isEqualTo(MeetingStatus.CLOSED);
        assertThat(response.getClosedAt()).isNotNull();
        verify(eventPublisher).publish(eq("ABC123"), any(), any());
    }

    @Test
    @DisplayName("Should throw BusinessRuleViolationException when closing an already closed meeting")
    void closeMeeting_AlreadyClosed() {
        meeting.setStatus(MeetingStatus.CLOSED);
        when(meetingRepository.findByMeetingCode("ABC123")).thenReturn(Optional.of(meeting));

        assertThatThrownBy(() -> meetingService.closeMeeting(10L, "ABC123"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Meeting is already closed");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when unauthorized organizer accesses meeting")
    void closeMeeting_UnauthorizedOrganizer() {
        Organizer otherOrganizer = Organizer.builder().id(999L).build();
        meeting.setOrganizer(otherOrganizer);

        when(meetingRepository.findByMeetingCode("ABC123")).thenReturn(Optional.of(meeting));

        assertThatThrownBy(() -> meetingService.closeMeeting(10L, "ABC123"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Meeting not found");
    }
}
