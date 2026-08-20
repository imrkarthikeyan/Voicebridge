package com.voicebridge.repository;

import com.voicebridge.entity.Presentation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PresentationRepository extends JpaRepository<Presentation, Long> {

    List<Presentation> findByMeetingIdOrderByUploadedAtDesc(Long meetingId);
}
