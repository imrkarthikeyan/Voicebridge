package com.voicebridge.controller;

import com.voicebridge.dto.request.JoinMeetingRequest;
import com.voicebridge.dto.response.ParticipantResponse;
import com.voicebridge.exception.InvalidCredentialsException;
import com.voicebridge.security.OrganizerPrincipal;
import com.voicebridge.service.ParticipantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Participants", description = "Audience join, session lookup, and organizer participant listing")
public class ParticipantController {

    private static final String SESSION_TOKEN_HEADER = "X-Participant-Token";

    private final ParticipantService participantService;

    @PostMapping("/api/participants/join/{meetingCode}")
    @Operation(summary = "Join a meeting as an audience member using the meeting code")
    public ResponseEntity<ParticipantResponse> join(@PathVariable String meetingCode,
                                                     @Valid @RequestBody JoinMeetingRequest request) {
        ParticipantResponse response = participantService.join(meetingCode, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping({"/api/participants/me", "/api/participants/session/{token}"})
    @Operation(summary = "Get the current participant's session info")
    public ResponseEntity<ParticipantResponse> getMe(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionTokenHeader,
            @RequestHeader(value = "X-Participant-Token", required = false) String participantTokenHeader,
            @PathVariable(value = "token", required = false) String tokenPath) {
        String token = sessionTokenHeader != null ? sessionTokenHeader : (participantTokenHeader != null ? participantTokenHeader : tokenPath);
        return ResponseEntity.ok(participantService.getBySessionToken(token));
    }

    @PostMapping("/api/participants/leave")
    @Operation(summary = "Leave the meeting and release the participant's session")
    public ResponseEntity<Void> leave(
            @RequestHeader(value = "X-Session-Token", required = false) String sessionTokenHeader,
            @RequestHeader(value = "X-Participant-Token", required = false) String participantTokenHeader) {
        String token = sessionTokenHeader != null ? sessionTokenHeader : participantTokenHeader;
        participantService.leave(token);
        return ResponseEntity.noContent().build();
    }

    @GetMapping({"/api/meetings/{meetingId}/participants", "/api/participants/meeting/{meetingId}"})
    @Operation(summary = "List all participants in a meeting (organizer only)")
    public ResponseEntity<List<ParticipantResponse>> listParticipants(
            @AuthenticationPrincipal OrganizerPrincipal principal,
            @PathVariable Long meetingId) {
        if (principal == null) {
            throw new InvalidCredentialsException("Organizer authentication required");
        }
        return ResponseEntity.ok(participantService.listParticipants(principal.getId(), meetingId));
    }
}
