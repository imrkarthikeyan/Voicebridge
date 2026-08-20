package com.voicebridge.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publish(String meetingCode, MeetingEventType type, Object payload) {
        MeetingEvent event = MeetingEvent.builder()
                .type(type)
                .payload(payload)
                .timestamp(Instant.now())
                .build();

        String destination = "/topic/meetings/" + meetingCode;
        log.debug("Publishing {} to {}", type, destination);
        messagingTemplate.convertAndSend(destination, event);
    }
}
