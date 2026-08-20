package com.voicebridge.websocket;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@SpringBootTest
class MeetingEventPublisherTest {

    @Autowired
    private MeetingEventPublisher eventPublisher;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    @Test
    void publish_sendsToMeetingSpecificTopic() {
        eventPublisher.publish("ABC123", MeetingEventType.HAND_RAISED, "payload");

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(org.mockito.ArgumentMatchers.eq("/topic/meetings/ABC123"),
                payloadCaptor.capture());

        MeetingEvent sent = (MeetingEvent) payloadCaptor.getValue();
        assertThat(sent.getType()).isEqualTo(MeetingEventType.HAND_RAISED);
        assertThat(sent.getPayload()).isEqualTo("payload");
        assertThat(sent.getTimestamp()).isNotNull();
    }
}
