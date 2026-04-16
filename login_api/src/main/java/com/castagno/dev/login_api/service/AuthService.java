package com.castagno.dev.login_api.service;

import com.castagno.dev.login_api.dto.request.LoginRequest;
import com.castagno.dev.login_api.dto.request.RegisterRequest;
import com.castagno.dev.login_api.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
