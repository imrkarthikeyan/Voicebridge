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
class SpeakingRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndLogin(String email) throws Exception {
        RegisterOrganizerRequest register = new RegisterOrganizerRequest();
        register.setName("Queue Test Organizer");
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

    private String joinAsParticipant(String meetingCode, String name) throws Exception {
        JoinMeetingRequest join = new JoinMeetingRequest();
        join.setName(name);

        MvcResult result = mockMvc.perform(post("/api/participants/join/{code}", meetingCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(join)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("sessionToken").asText();
    }

    @Test
    void fullQueueLifecycle_raiseApproveStartStop() throws Exception {
        String orgToken = registerAndLogin("queue-owner1@voicebridge.test");
        JsonNode meeting = createMeeting(orgToken, "Town Hall");
        String meetingCode = meeting.get("meetingCode").asText();
        long meetingId = meeting.get("id").asLong();

        String aliceToken = joinAsParticipant(meetingCode, "Alice");

        MvcResult raiseResult = mockMvc.perform(post("/api/speaking-requests/raise-hand")
                        .header("X-Participant-Token", aliceToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.queueOrder").value(1))
                .andReturn();

        long requestId = objectMapper.readTree(raiseResult.getResponse().getContentAsString()).get("id").asLong();

        // raising again should fail
        mockMvc.perform(post("/api/speaking-requests/raise-hand")
                        .header("X-Participant-Token", aliceToken))
                .andExpect(status().isConflict());

        // cannot start speaking before approval
        mockMvc.perform(post("/api/speaking-requests/me/start")
                        .header("X-Participant-Token", aliceToken))
                .andExpect(status().isConflict());

        // organizer approves
        mockMvc.perform(patch("/api/meetings/{meetingId}/speaking-requests/{id}/approve", meetingId, requestId)
                        .header("Authorization", "Bearer " + orgToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // participant starts speaking
        mockMvc.perform(post("/api/speaking-requests/me/start")
                        .header("X-Participant-Token", aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SPEAKING"));

        mockMvc.perform(get("/api/speaking-requests/me")
                        .header("X-Participant-Token", aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SPEAKING"));

        // participant stops speaking
        mockMvc.perform(post("/api/speaking-requests/me/stop")
                        .header("X-Participant-Token", aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"));

        // no longer has an active request
        mockMvc.perform(get("/api/speaking-requests/me")
                        .header("X-Participant-Token", aliceToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void onlyOneApprovedSpeakerAtATime() throws Exception {
        String orgToken = registerAndLogin("queue-owner2@voicebridge.test");
        JsonNode meeting = createMeeting(orgToken, "Panel Discussion");
        String meetingCode = meeting.get("meetingCode").asText();
        long meetingId = meeting.get("id").asLong();

        String aliceToken = joinAsParticipant(meetingCode, "Alice2");
        String bobToken = joinAsParticipant(meetingCode, "Bob2");

        MvcResult aliceRaise = mockMvc.perform(post("/api/speaking-requests/raise-hand")
                        .header("X-Participant-Token", aliceToken))
                .andExpect(status().isCreated())
                .andReturn();
        long aliceRequestId = objectMapper.readTree(aliceRaise.getResponse().getContentAsString()).get("id").asLong();

        MvcResult bobRaise = mockMvc.perform(post("/api/speaking-requests/raise-hand")
                        .header("X-Participant-Token", bobToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.queueOrder").value(2))
                .andReturn();
        long bobRequestId = objectMapper.readTree(bobRaise.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/meetings/{meetingId}/speaking-requests/{id}/approve", meetingId, aliceRequestId)
                        .header("Authorization", "Bearer " + orgToken))
                .andExpect(status().isOk());

        // Bob cannot be approved while Alice is approved/speaking
        mockMvc.perform(patch("/api/meetings/{meetingId}/speaking-requests/{id}/approve", meetingId, bobRequestId)
                        .header("Authorization", "Bearer " + orgToken))
                .andExpect(status().isConflict());

        // organizer force-ends Alice before she ever started speaking -> should fail (not SPEAKING yet)
        mockMvc.perform(patch("/api/meetings/{meetingId}/speaking-requests/{id}/end", meetingId, aliceRequestId)
                        .header("Authorization", "Bearer " + orgToken))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/speaking-requests/me/start")
                        .header("X-Participant-Token", aliceToken))
                .andExpect(status().isOk());

        // now organizer can force-end Alice
        mockMvc.perform(patch("/api/meetings/{meetingId}/speaking-requests/{id}/end", meetingId, aliceRequestId)
                        .header("Authorization", "Bearer " + orgToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"));

        // now Bob can be approved
        mockMvc.perform(patch("/api/meetings/{meetingId}/speaking-requests/{id}/approve", meetingId, bobRequestId)
                        .header("Authorization", "Bearer " + orgToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void organizerCanRejectAndReorderQueue() throws Exception {
        String orgToken = registerAndLogin("queue-owner3@voicebridge.test");
        JsonNode meeting = createMeeting(orgToken, "Community Meeting");
        String meetingCode = meeting.get("meetingCode").asText();
        long meetingId = meeting.get("id").asLong();

        String aliceToken = joinAsParticipant(meetingCode, "Alice3");
        String bobToken = joinAsParticipant(meetingCode, "Bob3");
        String carolToken = joinAsParticipant(meetingCode, "Carol3");

        long aliceRequestId = raiseHandAndGetId(aliceToken);
        long bobRequestId = raiseHandAndGetId(bobToken);
        long carolRequestId = raiseHandAndGetId(carolToken);

        // reject Bob
        mockMvc.perform(patch("/api/meetings/{meetingId}/speaking-requests/{id}/reject", meetingId, bobRequestId)
                        .header("Authorization", "Bearer " + orgToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        // reorder remaining waiting requests: Carol before Alice
        String reorderBody = objectMapper.writeValueAsString(
                new Object() {
                    public final long[] orderedRequestIds = {carolRequestId, aliceRequestId};
                });

        mockMvc.perform(patch("/api/meetings/{meetingId}/speaking-requests/reorder", meetingId)
                        .header("Authorization", "Bearer " + orgToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reorderBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + carolRequestId + ")].queueOrder").value(1))
                .andExpect(jsonPath("$[?(@.id == " + aliceRequestId + ")].queueOrder").value(2));
    }

    private long raiseHandAndGetId(String sessionToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/speaking-requests/raise-hand")
                        .header("X-Participant-Token", sessionToken))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }
}
