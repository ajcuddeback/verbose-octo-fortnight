package com.agentflow.service;

import com.agentflow.config.CreditProperties;
import com.agentflow.dto.AuthResponse;
import com.agentflow.dto.LoginRequest;
import com.agentflow.dto.RegisterRequest;
import com.agentflow.model.CreditBalance;
import com.agentflow.model.Subscription;
import com.agentflow.model.User;
import com.agentflow.model.enums.SubscriptionPlan;
import com.agentflow.model.enums.SubscriptionStatus;
import com.agentflow.model.enums.UserRole;
import com.agentflow.repository.CreditBalanceRepository;
import com.agentflow.repository.SubscriptionRepository;
import com.agentflow.repository.UserRepository;
import com.agentflow.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CreditBalanceRepository creditBalanceRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final CreditProperties creditProperties;

    @Transactional
    public ResponseEntity<AuthResponse> register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use: " + request.getEmail());
        }

        User user = userRepository.save(User.builder()
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .role(UserRole.USER)
            .build());

        // Grant free monthly credits
        creditBalanceRepository.save(CreditBalance.builder()
            .user(user)
            .balance(creditProperties.getFreeMonthlyAllowance())
            .monthlyAllowance(creditProperties.getFreeMonthlyAllowance())
            .lastResetDate(LocalDate.now())
            .build());

        // Create free subscription record
        subscriptionRepository.save(Subscription.builder()
            .user(user)
            .plan(SubscriptionPlan.FREE)
            .status(SubscriptionStatus.ACTIVE)
            .currentPeriodStart(LocalDateTime.now())
            .currentPeriodEnd(LocalDateTime.now().plusMonths(1))
            .build());

        String token = jwtTokenProvider.generateToken(user);
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, jwtTokenProvider.generateJwtCookie(token).toString())
            .body(toAuthResponse(user));
    }

    public ResponseEntity<AuthResponse> login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new IllegalStateException("User not found after authentication"));

        String token = jwtTokenProvider.generateToken(user);
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, jwtTokenProvider.generateJwtCookie(token).toString())
            .body(toAuthResponse(user));
    }

    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, jwtTokenProvider.generateLogoutCookie().toString())
            .body(Map.of("message", "Logged out successfully"));
    }

    public AuthResponse getMe(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalStateException("User not found: " + email));
        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(User user) {
        return AuthResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .role(user.getRole().name())
            .build();
    }
}
