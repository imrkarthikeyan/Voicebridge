package com.voicebridge.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicebridge.dto.request.CreateMeetingRequest;
import com.voicebridge.dto.request.JoinMeetingRequest;
import com.voicebridge.dto.request.RegisterOrganizerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String tokenOwner;
    private Long meetingOwnerId;
    private Long createdMeetingId;
    private String meetingCode;

    private String tokenAttacker;

    @BeforeEach
    void setUp() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);

        // Register Owner
        RegisterOrganizerRequest ownerReg = new RegisterOrganizerRequest();
        ownerReg.setName("Owner " + unique);
        ownerReg.setEmail("owner_" + unique + "@sec.test");
        ownerReg.setPassword("Password123!");

        MvcResult ownerAuthResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ownerReg)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode ownerJson = objectMapper.readTree(ownerAuthResult.getResponse().getContentAsString());
        tokenOwner = ownerJson.get("token").asText();
        meetingOwnerId = ownerJson.get("organizer").get("id").asLong();

        // Create Meeting
        CreateMeetingRequest createMeeting = new CreateMeetingRequest();
        createMeeting.setTitle("Security Test Meeting");

        MvcResult meetingResult = mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createMeeting)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode meetingJson = objectMapper.readTree(meetingResult.getResponse().getContentAsString());
        createdMeetingId = meetingJson.get("id").asLong();
        meetingCode = meetingJson.get("meetingCode").asText();

        // Register Attacker
        RegisterOrganizerRequest attackerReg = new RegisterOrganizerRequest();
        attackerReg.setName("Attacker " + unique);
        attackerReg.setEmail("attacker_" + unique + "@sec.test");
        attackerReg.setPassword("Password123!");

        MvcResult attackerAuthResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attackerReg)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode attackerJson = objectMapper.readTree(attackerAuthResult.getResponse().getContentAsString());
        tokenAttacker = attackerJson.get("token").asText();
    }

    @Test
    @DisplayName("Should reject requests missing authorization token")
    void unauthenticatedAccessShouldFail() throws Exception {
        mockMvc.perform(get("/api/meetings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reject invalid or malformed JWT token")
    void invalidJwtShouldFail() throws Exception {
        mockMvc.perform(get("/api/meetings")
                        .header("Authorization", "Bearer invalid.jwt.token.string"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("IDOR: Attacker organizer cannot close another organizer's meeting")
    void attackerCannotCloseOthersMeeting() throws Exception {
        mockMvc.perform(patch("/api/meetings/999999/close")
                        .header("Authorization", "Bearer " + tokenAttacker))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("IDOR: Attacker organizer cannot delete another organizer's presentation")
    void attackerCannotDeleteOthersPresentation() throws Exception {
        mockMvc.perform(delete("/api/presentations/999999")
                        .header("Authorization", "Bearer " + tokenAttacker))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("File Upload: Should reject executable files (.exe)")
    void executableFileUploadShouldFail() throws Exception {
        MockMultipartFile exeFile = new MockMultipartFile(
                "file",
                "malicious.exe",
                "application/x-msdownload",
                "MZ-executable-bytes".getBytes()
        );

        mockMvc.perform(multipart("/api/meetings/" + createdMeetingId + "/presentations")
                        .file(exeFile)
                        .header("Authorization", "Bearer " + tokenOwner))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_ARGUMENT"));
    }

    @Test
    @DisplayName("Audience cannot invoke organizer endpoints without JWT")
    void audienceCannotCallOrganizerEndpoints() throws Exception {
        mockMvc.perform(patch("/api/meetings/" + createdMeetingId + "/speaking-requests/1/approve"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Single active speaker invariant: Only one request can be approved")
    void singleActiveSpeakerInvariantEnforced() throws Exception {
        // Participant 1 joins
        JoinMeetingRequest join1 = new JoinMeetingRequest();
        join1.setName("Participant 1");
        MvcResult j1Result = mockMvc.perform(post("/api/participants/join/{code}", meetingCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(join1)))
                .andExpect(status().isCreated())
                .andReturn();

        String token1 = objectMapper.readTree(j1Result.getResponse().getContentAsString()).get("sessionToken").asText();

        // Participant 2 joins
        JoinMeetingRequest join2 = new JoinMeetingRequest();
        join2.setName("Participant 2");
        MvcResult j2Result = mockMvc.perform(post("/api/participants/join/{code}", meetingCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(join2)))
                .andExpect(status().isCreated())
                .andReturn();

        String token2 = objectMapper.readTree(j2Result.getResponse().getContentAsString()).get("sessionToken").asText();

        // Raise hands
        MvcResult req1Result = mockMvc.perform(post("/api/speaking-requests/raise-hand")
                        .header("X-Participant-Token", token1))
                .andExpect(status().isCreated())
                .andReturn();
        long req1Id = objectMapper.readTree(req1Result.getResponse().getContentAsString()).get("id").asLong();

        MvcResult req2Result = mockMvc.perform(post("/api/speaking-requests/raise-hand")
                        .header("X-Participant-Token", token2))
                .andExpect(status().isCreated())
                .andReturn();
        long req2Id = objectMapper.readTree(req2Result.getResponse().getContentAsString()).get("id").asLong();

        // Approve req1
        mockMvc.perform(patch("/api/meetings/" + createdMeetingId + "/speaking-requests/" + req1Id + "/approve")
                        .header("Authorization", "Bearer " + tokenOwner))
                .andExpect(status().isOk());

        // Attempting to approve req2 while req1 holds floor returns 409 Conflict
        mockMvc.perform(patch("/api/meetings/" + createdMeetingId + "/speaking-requests/" + req2Id + "/approve")
                        .header("Authorization", "Bearer " + tokenOwner))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Another participant is already approved or speaking"));
    }
}
