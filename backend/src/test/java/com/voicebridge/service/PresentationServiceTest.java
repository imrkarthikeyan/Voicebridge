package com.voicebridge.service;

import com.voicebridge.dto.request.ChangeSlideRequest;
import com.voicebridge.dto.response.PresentationSessionResponse;
import com.voicebridge.entity.Meeting;
import com.voicebridge.entity.enums.MeetingStatus;
import com.voicebridge.entity.Organizer;
import com.voicebridge.entity.Presentation;
import com.voicebridge.entity.PresentationSession;
import com.voicebridge.mapper.PresentationMapper;
import com.voicebridge.mapper.PresentationSessionMapper;
import com.voicebridge.repository.MeetingRepository;
import com.voicebridge.repository.PresentationRepository;
import com.voicebridge.repository.PresentationSessionRepository;
import com.voicebridge.service.impl.PresentationServiceImpl;
import com.voicebridge.websocket.MeetingEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PresentationServiceTest {

    @Mock
    private PresentationRepository presentationRepository;

    @Mock
    private PresentationSessionRepository sessionRepository;

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private PresentationStorageService storageService;

    @Mock
    private SlideProcessorService slideProcessorService;

    @Mock
    private PresentationMapper presentationMapper;

    @Mock
    private PresentationSessionMapper sessionMapper;

    @Mock
    private MeetingEventPublisher eventPublisher;

    @InjectMocks
    private PresentationServiceImpl presentationService;

    private Organizer organizer;
    private Meeting meeting;
    private Presentation presentation;
    private PresentationSession session;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(presentationService, "maxFileSize", 52428800L);

        organizer = Organizer.builder().id(1L).email("org@test.com").build();
        meeting = Meeting.builder().id(10L).meetingCode("MTEST").status(MeetingStatus.ACTIVE).organizer(organizer).build();

        presentation = Presentation.builder()
                .id(100L)
                .meeting(meeting)
                .fileName("slides.pptx")
                .fileType("PPTX")
                .storagePath("uploads/presentations/uuid/slides.pptx")
                .totalSlides(5)
                .currentSlide(1)
                .uploadedAt(Instant.now())
                .build();

        session = PresentationSession.builder()
                .id(200L)
                .presentation(presentation)
                .meeting(meeting)
                .presenting(true)
                .currentSlide(1)
                .build();
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for unsupported file extensions")
    void uploadPresentation_UnsupportedFormat() {
        MockMultipartFile exeFile = new MockMultipartFile("file", "script.exe", "application/octet-stream", new byte[]{1, 2, 3});

        when(meetingRepository.findById(10L)).thenReturn(Optional.of(meeting));

        assertThatThrownBy(() -> presentationService.uploadPresentation(1L, 10L, exeFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported file format");
    }

    @Test
    @DisplayName("Should start presentation session and publish PRESENTATION_STARTED event")
    void startPresentation_Success() {
        when(presentationRepository.findById(100L)).thenReturn(Optional.of(presentation));
        when(meetingRepository.findById(10L)).thenReturn(Optional.of(meeting));
        when(sessionRepository.findByMeetingId(10L)).thenReturn(Optional.empty());
        when(sessionRepository.save(any())).thenReturn(session);

        PresentationSessionResponse activeResponse = PresentationSessionResponse.builder().presenting(true).currentSlide(1).build();
        when(sessionMapper.toResponse(any())).thenReturn(activeResponse);

        PresentationSessionResponse response = presentationService.startPresentation(1L, 100L);

        assertThat(response.isPresenting()).isTrue();
        assertThat(response.getCurrentSlide()).isEqualTo(1);
        verify(eventPublisher).publish(eq("MTEST"), any(), any());
    }

    @Test
    @DisplayName("Should change slide within valid range [1, totalSlides]")
    void changeSlide_Success() {
        when(presentationRepository.findById(100L)).thenReturn(Optional.of(presentation));
        when(meetingRepository.findById(10L)).thenReturn(Optional.of(meeting));
        when(sessionRepository.findByMeetingId(10L)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PresentationSessionResponse changedResponse = PresentationSessionResponse.builder().presenting(true).currentSlide(3).build();
        when(sessionMapper.toResponse(any())).thenReturn(changedResponse);

        ChangeSlideRequest changeRequest = new ChangeSlideRequest(3);
        PresentationSessionResponse response = presentationService.changeSlide(1L, 100L, changeRequest);

        assertThat(response.getCurrentSlide()).isEqualTo(3);
        verify(eventPublisher).publish(eq("MTEST"), any(), any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when target slide is out of bounds")
    void changeSlide_OutOfBounds() {
        when(presentationRepository.findById(100L)).thenReturn(Optional.of(presentation));
        when(meetingRepository.findById(10L)).thenReturn(Optional.of(meeting));

        ChangeSlideRequest invalidRequest = new ChangeSlideRequest(99);

        assertThatThrownBy(() -> presentationService.changeSlide(1L, 100L, invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is out of range");
    }

    @Test
    @DisplayName("Should stop presentation session and publish PRESENTATION_STOPPED event")
    void stopPresentation_Success() {
        when(presentationRepository.findById(100L)).thenReturn(Optional.of(presentation));
        when(meetingRepository.findById(10L)).thenReturn(Optional.of(meeting));
        when(sessionRepository.findByMeetingId(10L)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PresentationSessionResponse stoppedResponse = PresentationSessionResponse.builder().presenting(false).currentSlide(1).build();
        when(sessionMapper.toResponse(any())).thenReturn(stoppedResponse);

        PresentationSessionResponse response = presentationService.stopPresentation(1L, 100L);

        assertThat(response.isPresenting()).isFalse();
        verify(eventPublisher).publish(eq("MTEST"), any(), any());
    }
}
