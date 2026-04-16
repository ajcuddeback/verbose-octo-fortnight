package com.budgetapp.controller;

import com.budgetapp.dto.request.LoginRequest;
import com.budgetapp.dto.request.RegisterRequest;
import com.budgetapp.dto.response.AuthResponse;
import com.budgetapp.dto.response.UserResponse;
import com.budgetapp.security.JwtTokenProvider;
import com.budgetapp.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
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
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Register a new user.
     * The JWT is placed in an HttpOnly, Secure, SameSite=Strict cookie — never in the response body.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        Object[] result = authService.register(request);
        String token        = (String)       result[0];
        AuthResponse body   = (AuthResponse) result[1];

        ResponseCookie jwtCookie = jwtTokenProvider.generateJwtCookie(token);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body(body);
    }

    /**
     * Log in with email + password.
     * The JWT is placed in an HttpOnly, Secure, SameSite=Strict cookie — never in the response body.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        Object[] result = authService.login(request);
        String token        = (String)       result[0];
        AuthResponse body   = (AuthResponse) result[1];

        ResponseCookie jwtCookie = jwtTokenProvider.generateJwtCookie(token);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body(body);
    }

    /**
     * Get the current authenticated user's profile.
     * The browser automatically sends the HttpOnly cookie; no token handling in JS.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {
        UserResponse response = authService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Log out by clearing the HttpOnly JWT cookie server-side.
     * After this, the browser has no cookie and all subsequent requests return 401.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        ResponseCookie clearCookie = jwtTokenProvider.generateLogoutCookie();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .body(Map.of("message", "Logged out successfully"));
    }
}
