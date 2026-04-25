package com.agentflow.controller;

import com.agentflow.dto.AuthResponse;
import com.agentflow.dto.LoginRequest;
import com.agentflow.dto.RegisterRequest;
import com.agentflow.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        return authService.logout();
    }

    @GetMapping("/me")
    public AuthResponse me(@AuthenticationPrincipal UserDetails principal) {
        return authService.getMe(principal.getUsername());
    }
}
