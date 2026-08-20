package com.voicebridge.repository;

import com.voicebridge.entity.SpeakerHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpeakerHistoryRepository extends JpaRepository<SpeakerHistory, Long> {

    List<SpeakerHistory> findByMeetingIdOrderByStartedAtAsc(Long meetingId);
}
