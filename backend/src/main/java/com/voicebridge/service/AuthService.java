package com.voicebridge.service;

import com.voicebridge.dto.request.LoginRequest;
import com.voicebridge.dto.request.RegisterOrganizerRequest;
import com.voicebridge.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterOrganizerRequest request);

    AuthResponse login(LoginRequest request);
}
