package com.voicebridge.repository;

import com.voicebridge.entity.PresentationSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PresentationSessionRepository extends JpaRepository<PresentationSession, Long> {

    Optional<PresentationSession> findByMeetingId(Long meetingId);

    Optional<PresentationSession> findByPresentationId(Long presentationId);
}
