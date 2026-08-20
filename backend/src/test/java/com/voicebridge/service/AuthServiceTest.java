package com.voicebridge.service;

import com.voicebridge.dto.request.LoginRequest;
import com.voicebridge.dto.request.RegisterOrganizerRequest;
import com.voicebridge.dto.response.AuthResponse;
import com.voicebridge.dto.response.OrganizerSummaryResponse;
import com.voicebridge.entity.Organizer;
import com.voicebridge.exception.DuplicateResourceException;
import com.voicebridge.exception.InvalidCredentialsException;
import com.voicebridge.mapper.OrganizerMapper;
import com.voicebridge.repository.OrganizerRepository;
import com.voicebridge.security.JwtService;
import com.voicebridge.security.OrganizerPrincipal;
import com.voicebridge.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private OrganizerRepository organizerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private OrganizerMapper organizerMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterOrganizerRequest registerRequest;
    private LoginRequest loginRequest;
    private Organizer organizer;
    private OrganizerSummaryResponse organizerResponse;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterOrganizerRequest();
        registerRequest.setName("Jane Organizer");
        registerRequest.setEmail("jane@test.com");
        registerRequest.setPassword("Secret123!");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("jane@test.com");
        loginRequest.setPassword("Secret123!");

        organizer = Organizer.builder()
                .id(1L)
                .name("Jane Organizer")
                .email("jane@test.com")
                .passwordHash("encoded_secret_hash")
                .build();

        organizerResponse = OrganizerSummaryResponse.builder()
                .id(1L)
                .name("Jane Organizer")
                .email("jane@test.com")
                .build();
    }

    @Test
    @DisplayName("Should successfully register a new organizer")
    void register_Success() {
        when(organizerRepository.existsByEmail("jane@test.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret123!")).thenReturn("encoded_secret_hash");
        when(organizerRepository.save(any(Organizer.class))).thenReturn(organizer);
        when(jwtService.generateToken(any(OrganizerPrincipal.class))).thenReturn("mock.jwt.token");
        when(organizerMapper.toSummaryResponse(any(Organizer.class))).thenReturn(organizerResponse);

        AuthResponse response = authService.register(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mock.jwt.token");
        assertThat(response.getOrganizer().getEmail()).isEqualTo("jane@test.com");
        verify(organizerRepository).save(any(Organizer.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when registering duplicate email")
    void register_DuplicateEmail() {
        when(organizerRepository.existsByEmail("jane@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("An organizer with this email already exists");
    }

    @Test
    @DisplayName("Should successfully authenticate valid login credentials")
    void login_Success() {
        when(organizerRepository.findByEmail("jane@test.com")).thenReturn(Optional.of(organizer));
        when(passwordEncoder.matches("Secret123!", "encoded_secret_hash")).thenReturn(true);
        when(jwtService.generateToken(any(OrganizerPrincipal.class))).thenReturn("mock.jwt.token");
        when(organizerMapper.toSummaryResponse(any(Organizer.class))).thenReturn(organizerResponse);

        AuthResponse response = authService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mock.jwt.token");
        assertThat(response.getOrganizer().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when login email is not found")
    void login_EmailNotFound() {
        when(organizerRepository.findByEmail("jane@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when password does not match")
    void login_WrongPassword() {
        when(organizerRepository.findByEmail("jane@test.com")).thenReturn(Optional.of(organizer));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid email or password");
    }
}
