package com.voicebridge.service;

import com.voicebridge.dto.request.CreateMeetingRequest;
import com.voicebridge.dto.request.JoinMeetingRequest;
import com.voicebridge.dto.request.RegisterOrganizerRequest;
import com.voicebridge.entity.SpeakingRequest;
import com.voicebridge.entity.enums.SpeakingRequestStatus;
import com.voicebridge.repository.SpeakingRequestRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext
public class SpeakingRequestConcurrencyTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private MeetingService meetingService;

    @Autowired
    private ParticipantService participantService;

    @Autowired
    private SpeakingRequestService speakingRequestService;

    @Autowired
    private SpeakingRequestRepository speakingRequestRepository;

    @Test
    @DisplayName("CONCURRENCY: Simultaneous approval attempts must ensure count(APPROVED or SPEAKING) <= 1")
    void concurrentSpeakerApproval_EnforcesSingleActiveSpeakerInvariant() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);

        // 1. Register Organizer
        RegisterOrganizerRequest reg = new RegisterOrganizerRequest();
        reg.setName("Concurrent Organizer " + unique);
        reg.setEmail("concurrent_" + unique + "@test.com");
        reg.setPassword("Password123!");
        var authResp = authService.register(reg);
        Long organizerId = authResp.getOrganizer().getId();

        // 2. Create Meeting
        CreateMeetingRequest createMeeting = new CreateMeetingRequest();
        createMeeting.setTitle("Concurrency Test Meeting");
        var meetingResp = meetingService.createMeeting(organizerId, createMeeting);
        Long meetingId = meetingResp.getId();
        String meetingCode = meetingResp.getMeetingCode();

        // 3. Join Participant 1 & Raise Hand
        JoinMeetingRequest join1 = new JoinMeetingRequest();
        join1.setName("Participant A");
        var p1 = participantService.join(meetingCode, join1);
        var req1 = speakingRequestService.raiseHand(p1.getSessionToken());

        // 4. Join Participant 2 & Raise Hand
        JoinMeetingRequest join2 = new JoinMeetingRequest();
        join2.setName("Participant B");
        var p2 = participantService.join(meetingCode, join2);
        var req2 = speakingRequestService.raiseHand(p2.getSessionToken());

        // 5. Execute Simultaneous Concurrent Approvals via ExecutorService
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        Future<?> future1 = executor.submit(() -> {
            try {
                startLatch.await();
                speakingRequestService.approve(organizerId, meetingId, req1.getId());
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            }
        });

        Future<?> future2 = executor.submit(() -> {
            try {
                startLatch.await();
                speakingRequestService.approve(organizerId, meetingId, req2.getId());
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            }
        });

        // Release both threads at once
        startLatch.countDown();

        future1.get(5, TimeUnit.SECONDS);
        future2.get(5, TimeUnit.SECONDS);

        executor.shutdown();

        // 6. Verification: Exactly 1 approval succeeded and 1 failed
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(1);

        // 7. Verify Database Invariant: count(APPROVED or SPEAKING) <= 1
        List<SpeakingRequest> activeOrApproved = speakingRequestRepository
                .findByMeetingIdOrderByQueuePositionAsc(meetingId).stream()
                .filter(r -> r.getStatus() == SpeakingRequestStatus.APPROVED || r.getStatus() == SpeakingRequestStatus.SPEAKING)
                .toList();

        assertThat(activeOrApproved).hasSize(1);
    }
}
