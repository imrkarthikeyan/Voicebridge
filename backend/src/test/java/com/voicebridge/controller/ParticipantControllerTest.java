package com.voicebridge.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicebridge.dto.request.CreateMeetingRequest;
import com.voicebridge.dto.request.JoinMeetingRequest;
import com.voicebridge.dto.request.RegisterOrganizerRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ParticipantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndLogin(String email) throws Exception {
        RegisterOrganizerRequest register = new RegisterOrganizerRequest();
        register.setName("Participant Test Organizer");
        register.setEmail(email);
        register.setPassword("SecurePass123");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private JsonNode createMeeting(String token, String title) throws Exception {
        CreateMeetingRequest request = new CreateMeetingRequest();
        request.setTitle(title);

        MvcResult result = mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    @Test
    void joinMeeting_thenFetchSelfAndLeave() throws Exception {
        String token = registerAndLogin("participant-owner1@voicebridge.test");
        JsonNode meeting = createMeeting(token, "Q&A Session");
        String meetingCode = meeting.get("meetingCode").asText();

        JoinMeetingRequest join = new JoinMeetingRequest();
        join.setName("Alice");

        MvcResult joinResult = mockMvc.perform(post("/api/participants/join/{code}", meetingCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(join)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.sessionToken").exists())
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();

        String sessionToken = objectMapper.readTree(joinResult.getResponse().getContentAsString())
                .get("sessionToken").asText();

        mockMvc.perform(get("/api/participants/me")
                        .header("X-Participant-Token", sessionToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice"));

        // organizer can list participants
        long meetingId = meeting.get("id").asLong();
        mockMvc.perform(get("/api/meetings/{id}/participants", meetingId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Alice"));

        mockMvc.perform(post("/api/participants/leave")
                        .header("X-Participant-Token", sessionToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/participants/me")
                        .header("X-Participant-Token", sessionToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void joinMeeting_duplicateNameInSameMeeting_returnsConflict() throws Exception {
        String token = registerAndLogin("participant-owner2@voicebridge.test");
        JsonNode meeting = createMeeting(token, "Duplicate Name Test");
        String meetingCode = meeting.get("meetingCode").asText();

        JoinMeetingRequest join = new JoinMeetingRequest();
        join.setName("Bob");

        mockMvc.perform(post("/api/participants/join/{code}", meetingCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(join)))
                .andExpect(status().isCreated());

        // case-insensitive duplicate
        JoinMeetingRequest joinDuplicate = new JoinMeetingRequest();
        joinDuplicate.setName("bob");

        mockMvc.perform(post("/api/participants/join/{code}", meetingCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinDuplicate)))
                .andExpect(status().isConflict());
    }

    @Test
    void joinMeeting_closedMeeting_returnsConflict() throws Exception {
        String token = registerAndLogin("participant-owner3@voicebridge.test");
        JsonNode meeting = createMeeting(token, "Closed Meeting Test");
        String meetingCode = meeting.get("meetingCode").asText();
        long meetingId = meeting.get("id").asLong();

        mockMvc.perform(patch("/api/meetings/{id}/close", meetingId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        JoinMeetingRequest join = new JoinMeetingRequest();
        join.setName("LateJoiner");

        mockMvc.perform(post("/api/participants/join/{code}", meetingCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(join)))
                .andExpect(status().isConflict());
    }

    @Test
    void joinMeeting_unknownCode_returnsNotFound() throws Exception {
        JoinMeetingRequest join = new JoinMeetingRequest();
        join.setName("Ghost");

        mockMvc.perform(post("/api/participants/join/{code}", "NOPE99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(join)))
                .andExpect(status().isNotFound());
    }
}
