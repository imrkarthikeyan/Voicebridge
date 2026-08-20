package com.voicebridge.controller;

import com.voicebridge.dto.request.CreateMeetingRequest;
import com.voicebridge.dto.response.MeetingJoinInfoResponse;
import com.voicebridge.dto.response.MeetingResponse;
import com.voicebridge.exception.InvalidCredentialsException;
import com.voicebridge.security.OrganizerPrincipal;
import com.voicebridge.service.MeetingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
@Tag(name = "Meetings", description = "Meeting lifecycle management and QR joining")
public class MeetingController {

    private final MeetingService meetingService;

    private void ensureAuthenticated(OrganizerPrincipal principal) {
        if (principal == null) {
            throw new InvalidCredentialsException("Organizer authentication required");
        }
    }

    @PostMapping
    @Operation(summary = "Create a new meeting")
    public ResponseEntity<MeetingResponse> createMeeting(@AuthenticationPrincipal OrganizerPrincipal principal,
                                                           @Valid @RequestBody CreateMeetingRequest request) {
        ensureAuthenticated(principal);
        MeetingResponse response = meetingService.createMeeting(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List all meetings created by the current organizer")
    public ResponseEntity<List<MeetingResponse>> listMeetings(@AuthenticationPrincipal OrganizerPrincipal principal) {
        ensureAuthenticated(principal);
        return ResponseEntity.ok(meetingService.listMeetingsForOrganizer(principal.getId()));
    }

    @GetMapping("/{identifier}")
    @Operation(summary = "Get meeting details and status by ID or meeting code")
    public ResponseEntity<MeetingResponse> getMeeting(@AuthenticationPrincipal OrganizerPrincipal principal,
                                                       @PathVariable String identifier) {
        ensureAuthenticated(principal);
        return ResponseEntity.ok(meetingService.getMeeting(principal.getId(), identifier));
    }

    @RequestMapping(value = "/{identifier}/close", method = {RequestMethod.PATCH, RequestMethod.POST})
    @Operation(summary = "Close a meeting so no further joins or requests are accepted")
    public ResponseEntity<MeetingResponse> closeMeeting(@AuthenticationPrincipal OrganizerPrincipal principal,
                                                         @PathVariable String identifier) {
        ensureAuthenticated(principal);
        return ResponseEntity.ok(meetingService.closeMeeting(principal.getId(), identifier));
    }

    @GetMapping(value = "/{meetingCode}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "Get the QR code image (PNG) that encodes the audience join link")
    public ResponseEntity<byte[]> getQrCode(@PathVariable String meetingCode) {
        byte[] png = meetingService.generateQrCode(meetingCode);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
    }

    @GetMapping("/join/{meetingCode}")
    @Operation(summary = "Get basic meeting info before an audience member joins")
    public ResponseEntity<MeetingJoinInfoResponse> getJoinInfo(@PathVariable String meetingCode) {
        return ResponseEntity.ok(meetingService.getJoinInfo(meetingCode));
    }
}
