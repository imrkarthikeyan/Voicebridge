package com.voicebridge.webrtc;

import com.voicebridge.entity.Meeting;
import com.voicebridge.entity.enums.MeetingStatus;
import com.voicebridge.repository.MeetingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class WebRtcSignalingTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private MeetingRepository meetingRepository;

    @InjectMocks
    private WebRtcSignalingController signalingController;

    private Meeting activeMeeting;
    private Meeting closedMeeting;

    @BeforeEach
    void setUp() {
        activeMeeting = Meeting.builder().id(100L).meetingCode("M100").status(MeetingStatus.ACTIVE).build();
        closedMeeting = Meeting.builder().id(200L).meetingCode("M200").status(MeetingStatus.CLOSED).build();
    }

    @Test
    @DisplayName("Should relay valid OFFER signal from ORGANIZER to meeting signaling channel")
    void handleSignal_ValidOrganizerOffer() {
        when(meetingRepository.findByMeetingCode("M100")).thenReturn(Optional.of(activeMeeting));

        SignalMessage offer = new SignalMessage();
        offer.setType(SignalType.OFFER);
        offer.setFrom(SignalRole.ORGANIZER);
        offer.setSdp("v=0 offer-sdp");

        signalingController.relaySignal("M100", offer);

        verify(messagingTemplate).convertAndSend(eq("/topic/meetings/M100/signal"), eq(offer));
    }

    @Test
    @DisplayName("Should relay valid ANSWER signal from SPEAKER to meeting signaling channel")
    void handleSignal_ValidSpeakerAnswer() {
        when(meetingRepository.findByMeetingCode("M100")).thenReturn(Optional.of(activeMeeting));

        SignalMessage answer = new SignalMessage();
        answer.setType(SignalType.ANSWER);
        answer.setFrom(SignalRole.SPEAKER);
        answer.setSdp("v=0 answer-sdp");

        signalingController.relaySignal("M100", answer);

        verify(messagingTemplate).convertAndSend(eq("/topic/meetings/M100/signal"), eq(answer));
    }

    @Test
    @DisplayName("Should drop signaling message when signal type is null or unknown")
    void handleSignal_InvalidSignalTypeDropped() {
        SignalMessage invalidType = new SignalMessage();
        invalidType.setType(null);
        invalidType.setFrom(SignalRole.ORGANIZER);

        signalingController.relaySignal("M100", invalidType);

        verify(messagingTemplate, never()).convertAndSend(any(String.class), any(Object.class));
    }

    @Test
    @DisplayName("Should drop signaling message when meeting is CLOSED")
    void handleSignal_ClosedMeetingDropped() {
        when(meetingRepository.findByMeetingCode("M200")).thenReturn(Optional.of(closedMeeting));

        SignalMessage offer = new SignalMessage();
        offer.setType(SignalType.OFFER);
        offer.setFrom(SignalRole.ORGANIZER);

        signalingController.relaySignal("M200", offer);

        verify(messagingTemplate, never()).convertAndSend(any(String.class), any(Object.class));
    }
}
