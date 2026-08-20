package com.voicebridge.webrtc;

import com.voicebridge.entity.Meeting;
import com.voicebridge.entity.enums.MeetingStatus;
import com.voicebridge.repository.MeetingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Optional;
import java.util.Set;

/**
 * Relays WebRTC SDP offers/answers and ICE candidates between the organizer's
 * browser and the currently approved speaker's browser. The server never
 * inspects or stores media — it only forwards signaling payloads so the
 * actual audio stream can be established peer-to-peer.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class WebRtcSignalingController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MeetingRepository meetingRepository;

    private static final Set<String> ALLOWED_TYPES = Set.of("OFFER", "ANSWER", "ICE_CANDIDATE");
    private static final Set<String> ALLOWED_ROLES = Set.of("ORGANIZER", "SPEAKER");

    @MessageMapping("/meetings/{meetingCode}/signal")
    public void relaySignal(@DestinationVariable String meetingCode, SignalMessage message) {
        if (message == null || message.getType() == null || message.getFrom() == null) {
            log.warn("Dropping invalid/null WebRTC signal for meeting {}", meetingCode);
            return;
        }

        String typeStr = message.getType().name();
        String fromStr = message.getFrom().name();

        if (!ALLOWED_TYPES.contains(typeStr) || !ALLOWED_ROLES.contains(fromStr)) {
            log.warn("Dropping unauthorized signal type {} or role {} for meeting {}", typeStr, fromStr, meetingCode);
            return;
        }

        Optional<Meeting> meeting = meetingRepository.findByMeetingCode(meetingCode);

        if (meeting.isEmpty() || meeting.get().getStatus() == MeetingStatus.CLOSED) {
            log.debug("Dropping signal for unknown/closed meeting: {}", meetingCode);
            return;
        }

        log.debug("Relaying {} signal from {} in meeting {}", typeStr, fromStr, meetingCode);
        messagingTemplate.convertAndSend("/topic/meetings/" + meetingCode + "/signal", message);
    }
}
