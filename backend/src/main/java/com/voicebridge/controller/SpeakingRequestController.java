package com.voicebridge.controller;

import com.voicebridge.dto.request.ReorderQueueRequest;
import com.voicebridge.dto.response.SpeakingRequestResponse;
import com.voicebridge.exception.InvalidCredentialsException;
import com.voicebridge.security.OrganizerPrincipal;
import com.voicebridge.service.SpeakingRequestService;
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
@Tag(name = "Speaking Requests", description = "Raise-hand queue: audience requests and organizer moderation")
public class SpeakingRequestController {

    private static final String SESSION_TOKEN_HEADER = "X-Participant-Token";

    private final SpeakingRequestService speakingRequestService;

    private String resolveToken(String header1, String header2) {
        return header1 != null ? header1 : header2;
    }

    private void ensureAuthenticated(OrganizerPrincipal principal) {
        if (principal == null) {
            throw new InvalidCredentialsException("Organizer authentication required");
        }
    }

    // ---- Audience-facing endpoints (no auth; identified by session token) ----

    @PostMapping("/api/speaking-requests/raise-hand")
    @Operation(summary = "Raise a hand to request permission to speak")
    public ResponseEntity<SpeakingRequestResponse> raiseHand(
            @RequestHeader(value = "X-Session-Token", required = false) String token1,
            @RequestHeader(value = "X-Participant-Token", required = false) String token2) {
        SpeakingRequestResponse response = speakingRequestService.raiseHand(resolveToken(token1, token2));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping({"/api/speaking-requests/me", "/api/speaking-requests/my-status"})
    @Operation(summary = "Get the current participant's active speaking request status")
    public ResponseEntity<SpeakingRequestResponse> getMyRequest(
            @RequestHeader(value = "X-Session-Token", required = false) String token1,
            @RequestHeader(value = "X-Participant-Token", required = false) String token2) {
        return ResponseEntity.ok(speakingRequestService.getMyRequest(resolveToken(token1, token2)));
    }

    @PostMapping({"/api/speaking-requests/me/start", "/api/speaking-requests/start"})
    @Operation(summary = "Signal that the microphone is live after approval")
    public ResponseEntity<SpeakingRequestResponse> startSpeaking(
            @RequestHeader(value = "X-Session-Token", required = false) String token1,
            @RequestHeader(value = "X-Participant-Token", required = false) String token2) {
        return ResponseEntity.ok(speakingRequestService.startSpeaking(resolveToken(token1, token2)));
    }

    @PostMapping({"/api/speaking-requests/me/stop", "/api/speaking-requests/stop"})
    @Operation(summary = "Stop speaking and release the microphone")
    public ResponseEntity<SpeakingRequestResponse> stopSpeaking(
            @RequestHeader(value = "X-Session-Token", required = false) String token1,
            @RequestHeader(value = "X-Participant-Token", required = false) String token2) {
        return ResponseEntity.ok(speakingRequestService.stopSpeaking(resolveToken(token1, token2)));
    }

    // ---- Organizer-facing endpoints (JWT auth required) ----

    @GetMapping({"/api/meetings/{meetingId}/speaking-requests", "/api/speaking-requests/meeting/{meetingId}"})
    @Operation(summary = "List the full speaking request queue for a meeting")
    public ResponseEntity<List<SpeakingRequestResponse>> listQueue(
            @AuthenticationPrincipal OrganizerPrincipal principal,
            @PathVariable Long meetingId) {
        ensureAuthenticated(principal);
        return ResponseEntity.ok(speakingRequestService.listQueue(principal.getId(), meetingId));
    }

    @RequestMapping(value = {"/api/meetings/{meetingId}/speaking-requests/{requestId}/approve", "/api/speaking-requests/{requestId}/approve"}, method = {RequestMethod.PATCH, RequestMethod.POST})
    @Operation(summary = "Approve a waiting speaker")
    public ResponseEntity<SpeakingRequestResponse> approve(
            @AuthenticationPrincipal OrganizerPrincipal principal,
            @PathVariable(value = "meetingId", required = false) Long meetingId,
            @PathVariable("requestId") Long requestId) {
        ensureAuthenticated(principal);
        return ResponseEntity.ok(speakingRequestService.approve(principal.getId(), meetingId, requestId));
    }

    @RequestMapping(value = {"/api/meetings/{meetingId}/speaking-requests/{requestId}/reject", "/api/speaking-requests/{requestId}/reject"}, method = {RequestMethod.PATCH, RequestMethod.POST})
    @Operation(summary = "Reject a waiting or approved speaker")
    public ResponseEntity<SpeakingRequestResponse> reject(
            @AuthenticationPrincipal OrganizerPrincipal principal,
            @PathVariable(value = "meetingId", required = false) Long meetingId,
            @PathVariable("requestId") Long requestId) {
        ensureAuthenticated(principal);
        return ResponseEntity.ok(speakingRequestService.reject(principal.getId(), meetingId, requestId));
    }

    @RequestMapping(value = {"/api/meetings/{meetingId}/speaking-requests/{requestId}/end", "/api/speaking-requests/meeting/{meetingId}/end-speaker"}, method = {RequestMethod.PATCH, RequestMethod.POST})
    @Operation(summary = "Force-end the current speaker")
    public ResponseEntity<SpeakingRequestResponse> endSpeaker(
            @AuthenticationPrincipal OrganizerPrincipal principal,
            @PathVariable(value = "meetingId", required = false) Long meetingId,
            @PathVariable(value = "requestId", required = false) Long requestId) {
        ensureAuthenticated(principal);
        return ResponseEntity.ok(speakingRequestService.endSpeaker(principal.getId(), meetingId, requestId));
    }

    @RequestMapping(value = {"/api/meetings/{meetingId}/speaking-requests/reorder", "/api/speaking-requests/meeting/{meetingId}/reorder"}, method = {RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.POST})
    @Operation(summary = "Reorder the waiting queue")
    public ResponseEntity<List<SpeakingRequestResponse>> reorderQueue(
            @AuthenticationPrincipal OrganizerPrincipal principal,
            @PathVariable Long meetingId,
            @Valid @RequestBody ReorderQueueRequest request) {
        ensureAuthenticated(principal);
        return ResponseEntity.ok(speakingRequestService.reorderQueue(principal.getId(), meetingId, request));
    }
}
