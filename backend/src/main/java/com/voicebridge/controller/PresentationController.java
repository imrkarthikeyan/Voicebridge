package com.voicebridge.controller;

import com.voicebridge.dto.request.ChangeSlideRequest;
import com.voicebridge.dto.response.ApiResponse;
import com.voicebridge.dto.response.PresentationResponse;
import com.voicebridge.dto.response.PresentationSessionResponse;
import com.voicebridge.exception.InvalidCredentialsException;
import com.voicebridge.security.OrganizerPrincipal;
import com.voicebridge.service.PresentationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PresentationController {

    private final PresentationService presentationService;

    private void ensureAuthenticated(OrganizerPrincipal principal) {
        if (principal == null) {
            throw new InvalidCredentialsException("Organizer authentication required");
        }
    }

    @PostMapping(value = "/api/meetings/{meetingId}/presentations", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PresentationResponse>> uploadPresentation(
            @AuthenticationPrincipal OrganizerPrincipal principal,
            @PathVariable Long meetingId,
            @RequestParam("file") MultipartFile file) {

        ensureAuthenticated(principal);
        PresentationResponse response = presentationService.uploadPresentation(principal.getId(), meetingId, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Presentation uploaded and processed successfully", response));
    }

    @GetMapping("/api/meetings/{meetingId}/presentations")
    public ResponseEntity<ApiResponse<List<PresentationResponse>>> listPresentations(@PathVariable Long meetingId) {
        List<PresentationResponse> response = presentationService.listPresentations(meetingId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/api/presentations/{presentationId}")
    public ResponseEntity<ApiResponse<PresentationResponse>> getPresentation(@PathVariable Long presentationId) {
        PresentationResponse response = presentationService.getPresentation(presentationId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/api/presentations/{presentationId}")
    public ResponseEntity<ApiResponse<Void>> deletePresentation(
            @AuthenticationPrincipal OrganizerPrincipal principal,
            @PathVariable Long presentationId) {

        ensureAuthenticated(principal);
        presentationService.deletePresentation(principal.getId(), presentationId);
        return ResponseEntity.ok(ApiResponse.success("Presentation deleted successfully", null));
    }

    @PostMapping("/api/presentations/{presentationId}/start")
    public ResponseEntity<ApiResponse<PresentationSessionResponse>> startPresentation(
            @AuthenticationPrincipal OrganizerPrincipal principal,
            @PathVariable Long presentationId) {

        ensureAuthenticated(principal);
        PresentationSessionResponse response = presentationService.startPresentation(principal.getId(), presentationId);
        return ResponseEntity.ok(ApiResponse.success("Presentation started", response));
    }

    @PostMapping("/api/presentations/{presentationId}/stop")
    public ResponseEntity<ApiResponse<PresentationSessionResponse>> stopPresentation(
            @AuthenticationPrincipal OrganizerPrincipal principal,
            @PathVariable Long presentationId) {

        ensureAuthenticated(principal);
        PresentationSessionResponse response = presentationService.stopPresentation(principal.getId(), presentationId);
        return ResponseEntity.ok(ApiResponse.success("Presentation stopped", response));
    }

    @PutMapping("/api/presentations/{presentationId}/slide")
    public ResponseEntity<ApiResponse<PresentationSessionResponse>> changeSlide(
            @AuthenticationPrincipal OrganizerPrincipal principal,
            @PathVariable Long presentationId,
            @Valid @RequestBody ChangeSlideRequest request) {

        ensureAuthenticated(principal);
        PresentationSessionResponse response = presentationService.changeSlide(principal.getId(), presentationId, request);
        return ResponseEntity.ok(ApiResponse.success("Slide updated", response));
    }

    @GetMapping("/api/presentations/{presentationId}/session")
    public ResponseEntity<ApiResponse<PresentationSessionResponse>> getSession(@PathVariable Long presentationId) {
        PresentationSessionResponse response = presentationService.getSession(presentationId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/api/meetings/{meetingId}/presentation-session")
    public ResponseEntity<ApiResponse<PresentationSessionResponse>> getMeetingSession(@PathVariable Long meetingId) {
        PresentationSessionResponse response = presentationService.getMeetingSession(meetingId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping(value = "/api/presentations/{presentationId}/slides/{slideNumber}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getSlideImage(
            @PathVariable Long presentationId,
            @PathVariable Integer slideNumber) {

        byte[] imageBytes = presentationService.getSlideImage(presentationId, slideNumber);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(86400, TimeUnit.SECONDS).cachePublic())
                .contentType(MediaType.IMAGE_PNG)
                .body(imageBytes);
    }
}
