package com.voicebridge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicebridge.dto.request.LoginRequest;
import com.voicebridge.dto.request.RegisterOrganizerRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerThenLogin_succeeds() throws Exception {
        RegisterOrganizerRequest register = new RegisterOrganizerRequest();
        register.setName("Jane Organizer");
        register.setEmail("jane@voicebridge.test");
        register.setPassword("SecurePass123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.organizer.email").value("jane@voicebridge.test"));

        LoginRequest login = new LoginRequest();
        login.setEmail("jane@voicebridge.test");
        login.setPassword("SecurePass123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void register_duplicateEmail_returnsConflict() throws Exception {
        RegisterOrganizerRequest register = new RegisterOrganizerRequest();
        register.setName("Dup Organizer");
        register.setEmail("dup@voicebridge.test");
        register.setPassword("SecurePass123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("An organizer with this email already exists"));
    }

    @Test
    void login_wrongPassword_returnsUnauthorized() throws Exception {
        RegisterOrganizerRequest register = new RegisterOrganizerRequest();
        register.setName("Bad Login Organizer");
        register.setEmail("badlogin@voicebridge.test");
        register.setPassword("SecurePass123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest();
        login.setEmail("badlogin@voicebridge.test");
        login.setPassword("WrongPassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_invalidPayload_returnsBadRequestWithValidationErrors() throws Exception {
        RegisterOrganizerRequest register = new RegisterOrganizerRequest();
        register.setName("");
        register.setEmail("not-an-email");
        register.setPassword("short");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.email").exists())
                .andExpect(jsonPath("$.validationErrors.password").exists());
    }
}
