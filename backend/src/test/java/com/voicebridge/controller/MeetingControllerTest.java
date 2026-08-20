package com.voicebridge.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicebridge.dto.request.CreateMeetingRequest;
import com.voicebridge.dto.request.RegisterOrganizerRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class MeetingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndLogin(String email) throws Exception {
        RegisterOrganizerRequest register = new RegisterOrganizerRequest();
        register.setName("Meeting Organizer");
        register.setEmail(email);
        register.setPassword("SecurePass123");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("token").asText();
    }

    @Test
    void createMeeting_withoutAuth_isUnauthorized() throws Exception {
        CreateMeetingRequest request = new CreateMeetingRequest();
        request.setTitle("Placement Talk");

        mockMvc.perform(post("/api/meetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void fullMeetingLifecycle_createGetQrJoinInfoClose() throws Exception {
        String token = registerAndLogin("meeting1@voicebridge.test");

        CreateMeetingRequest request = new CreateMeetingRequest();
        request.setTitle("Placement Talk");

        MvcResult createResult = mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.meetingCode").exists())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long meetingId = created.get("id").asLong();
        String meetingCode = created.get("meetingCode").asText();
        assertThat(meetingCode).hasSize(6);

        mockMvc.perform(get("/api/meetings/{id}", meetingId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Placement Talk"));

        mockMvc.perform(get("/api/meetings"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/meetings")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].meetingCode").value(meetingCode));

        // QR code and join-info are public, no auth header needed
        MvcResult qrResult = mockMvc.perform(get("/api/meetings/{code}/qr", meetingCode))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andReturn();
        assertThat(qrResult.getResponse().getContentAsByteArray().length).isGreaterThan(0);

        mockMvc.perform(get("/api/meetings/join/{code}", meetingCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(patch("/api/meetings/{id}/close", meetingId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        // closing again should fail
        mockMvc.perform(patch("/api/meetings/{id}/close", meetingId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    void getMeeting_ownedByAnotherOrganizer_returnsNotFound() throws Exception {
        String tokenA = registerAndLogin("owner-a@voicebridge.test");
        String tokenB = registerAndLogin("owner-b@voicebridge.test");

        CreateMeetingRequest request = new CreateMeetingRequest();
        request.setTitle("Owner A Meeting");

        MvcResult createResult = mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        long meetingId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/meetings/{id}", meetingId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void getJoinInfo_unknownMeetingCode_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/meetings/join/{code}", "ZZZZZZ"))
                .andExpect(status().isNotFound());
    }
}
