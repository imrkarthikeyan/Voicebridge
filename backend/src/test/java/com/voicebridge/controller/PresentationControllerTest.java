package com.voicebridge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicebridge.dto.request.ChangeSlideRequest;
import com.voicebridge.dto.request.CreateMeetingRequest;
import com.voicebridge.dto.request.LoginRequest;
import com.voicebridge.dto.request.RegisterOrganizerRequest;
import com.voicebridge.dto.response.AuthResponse;
import com.voicebridge.dto.response.MeetingResponse;
import com.voicebridge.dto.response.PresentationResponse;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class PresentationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String tokenOwner;
    private String tokenOther;
    private Long meetingId;

    @BeforeEach
    void setUp() throws Exception {
        String ownerEmail = "pres-owner-" + System.currentTimeMillis() + "@test.com";
        RegisterOrganizerRequest reg1 = new RegisterOrganizerRequest();
        reg1.setName("Pres Owner");
        reg1.setEmail(ownerEmail);
        reg1.setPassword("Password123!");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg1)))
                .andExpect(status().isCreated());

        LoginRequest login1 = new LoginRequest();
        login1.setEmail(ownerEmail);
        login1.setPassword("Password123!");

        MvcResult loginResult1 = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login1)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse auth1 = objectMapper.readValue(loginResult1.getResponse().getContentAsString(), AuthResponse.class);
        tokenOwner = "Bearer " + auth1.getToken();

        String otherEmail = "pres-other-" + System.currentTimeMillis() + "@test.com";
        RegisterOrganizerRequest reg2 = new RegisterOrganizerRequest();
        reg2.setName("Pres Other");
        reg2.setEmail(otherEmail);
        reg2.setPassword("Password123!");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg2)))
                .andExpect(status().isCreated());

        LoginRequest login2 = new LoginRequest();
        login2.setEmail(otherEmail);
        login2.setPassword("Password123!");

        MvcResult loginResult2 = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login2)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse auth2 = objectMapper.readValue(loginResult2.getResponse().getContentAsString(), AuthResponse.class);
        tokenOther = "Bearer " + auth2.getToken();

        CreateMeetingRequest meetingReq = new CreateMeetingRequest();
        meetingReq.setTitle("Presentation Test Meeting");
        meetingReq.setDescription("Testing Phase 9 APIs");

        MvcResult meetingResult = mockMvc.perform(post("/api/meetings")
                        .header("Authorization", tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(meetingReq)))
                .andExpect(status().isCreated())
                .andReturn();

        MeetingResponse meeting = objectMapper.readValue(meetingResult.getResponse().getContentAsString(), MeetingResponse.class);
        meetingId = meeting.getId();
    }

    private byte[] createSamplePptxBytes(int numSlides) throws Exception {
        try (XMLSlideShow ppt = new XMLSlideShow(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            for (int i = 0; i < numSlides; i++) {
                XSLFSlide slide = ppt.createSlide();
            }
            ppt.write(baos);
            return baos.toByteArray();
        }
    }

    @Test
    void testUploadPresentation_Success() throws Exception {
        byte[] pptxBytes = createSamplePptxBytes(3);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.pptx",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                pptxBytes
        );

        mockMvc.perform(multipart("/api/meetings/{meetingId}/presentations", meetingId)
                        .file(file)
                        .header("Authorization", tokenOwner))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.fileName", is("sample.pptx")))
                .andExpect(jsonPath("$.data.fileType", is("PPTX")))
                .andExpect(jsonPath("$.data.totalSlides", is(3)));
    }

    @Test
    void testUploadPresentation_UnsupportedFileFormat() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.ppt",
                "application/vnd.ms-powerpoint",
                "fake-ppt-content".getBytes()
        );

        mockMvc.perform(multipart("/api/meetings/{meetingId}/presentations", meetingId)
                        .file(file)
                        .header("Authorization", tokenOwner))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Unsupported file format")));
    }

    @Test
    void testPresentationLifecycleAndNavigation() throws Exception {
        byte[] pptxBytes = createSamplePptxBytes(4);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "deck.pptx",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                pptxBytes
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/meetings/{meetingId}/presentations", meetingId)
                        .file(file)
                        .header("Authorization", tokenOwner))
                .andExpect(status().isCreated())
                .andReturn();

        ApiResponseWrapper<PresentationResponse> uploaded = objectMapper.readValue(
                uploadResult.getResponse().getContentAsString(),
                ApiResponseWrapper.class
        );

        Long presentationId = ((Number) ((java.util.Map<?, ?>) uploaded.data).get("id")).longValue();

        mockMvc.perform(get("/api/meetings/{meetingId}/presentations", meetingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        mockMvc.perform(post("/api/presentations/{presentationId}/start", presentationId)
                        .header("Authorization", tokenOwner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.presenting", is(true)))
                .andExpect(jsonPath("$.data.currentSlide", is(1)));

        ChangeSlideRequest validSlide = new ChangeSlideRequest(3);
        mockMvc.perform(put("/api/presentations/{presentationId}/slide", presentationId)
                        .header("Authorization", tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validSlide)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentSlide", is(3)));

        ChangeSlideRequest invalidSlide = new ChangeSlideRequest(99);
        mockMvc.perform(put("/api/presentations/{presentationId}/slide", presentationId)
                        .header("Authorization", tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidSlide)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));

        mockMvc.perform(get("/api/presentations/{presentationId}/slides/3", presentationId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.IMAGE_PNG_VALUE));

        mockMvc.perform(post("/api/presentations/{presentationId}/stop", presentationId)
                        .header("Authorization", tokenOwner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.presenting", is(false)));

        mockMvc.perform(delete("/api/presentations/{presentationId}", presentationId)
                        .header("Authorization", tokenOwner))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/meetings/{meetingId}/presentations", meetingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void testUnauthorizedUser_CannotModifyPresentation() throws Exception {
        byte[] pptxBytes = createSamplePptxBytes(2);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "private.pptx",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                pptxBytes
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/meetings/{meetingId}/presentations", meetingId)
                        .file(file)
                        .header("Authorization", tokenOwner))
                .andExpect(status().isCreated())
                .andReturn();

        ApiResponseWrapper<PresentationResponse> uploaded = objectMapper.readValue(
                uploadResult.getResponse().getContentAsString(),
                ApiResponseWrapper.class
        );
        Long presentationId = ((Number) ((java.util.Map<?, ?>) uploaded.data).get("id")).longValue();

        mockMvc.perform(post("/api/presentations/{presentationId}/start", presentationId)
                        .header("Authorization", tokenOther))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/presentations/{presentationId}", presentationId)
                        .header("Authorization", tokenOther))
                .andExpect(status().isNotFound());
    }

    private static class ApiResponseWrapper<T> {
        public boolean success;
        public String message;
        public Object data;
    }
}
