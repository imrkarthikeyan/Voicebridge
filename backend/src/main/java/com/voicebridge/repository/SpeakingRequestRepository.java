package com.voicebridge.repository;

import com.voicebridge.entity.SpeakingRequest;
import com.voicebridge.entity.enums.SpeakingRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpeakingRequestRepository extends JpaRepository<SpeakingRequest, Long> {

    List<SpeakingRequest> findByMeetingIdAndStatusOrderByQueuePositionAsc(Long meetingId, SpeakingRequestStatus status);

    List<SpeakingRequest> findByMeetingIdOrderByQueuePositionAsc(Long meetingId);

    Optional<SpeakingRequest> findByMeetingIdAndStatus(Long meetingId, SpeakingRequestStatus status);

    Optional<SpeakingRequest> findFirstByMeetingIdAndStatusIn(Long meetingId, List<SpeakingRequestStatus> statuses);

    Optional<SpeakingRequest> findByParticipantIdAndStatusIn(Long participantId, List<SpeakingRequestStatus> statuses);

    boolean existsByParticipantIdAndStatusIn(Long participantId, List<SpeakingRequestStatus> statuses);

    long countByMeetingId(Long meetingId);
}
