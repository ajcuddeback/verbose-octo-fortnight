package com.budgetapp.service;

import com.budgetapp.dto.request.LoginRequest;
import com.budgetapp.dto.request.RegisterRequest;
import com.budgetapp.dto.response.AuthResponse;
import com.budgetapp.dto.response.UserResponse;
import com.budgetapp.exception.ResourceNotFoundException;
import com.budgetapp.model.Subscription;
import com.budgetapp.model.SubscriptionStatus;
import com.budgetapp.model.User;
import com.budgetapp.repository.SubscriptionRepository;
import com.budgetapp.repository.UserRepository;
import com.budgetapp.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /**
     * Typed container returned by register() and login().
     * The controller extracts the token for the HttpOnly cookie and
     * sends the AuthResponse as the response body.
     */
    public record AuthResult(String token, AuthResponse authResponse) {}

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    /**
     * Registers a new user and returns the generated JWT token alongside the AuthResponse.
     * The controller is responsible for placing the token in an HttpOnly cookie.
     */
    @Transactional
    public AuthResult register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered: " + request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .subscriptionStatus(SubscriptionStatus.FREE_TRIAL)
                .build();

        User savedUser = userRepository.save(user);
        log.info("New user registered: {}", savedUser.getEmail());

        // Create FREE_TRIAL subscription record
        Subscription trialSubscription = Subscription.builder()
                .user(savedUser)
                .status(SubscriptionStatus.FREE_TRIAL)
                .startDate(LocalDate.now())
                .amountCents(0)
                .build();
        subscriptionRepository.save(trialSubscription);

        // Send welcome email
        try {
            emailService.sendWelcomeEmail(savedUser);
        } catch (Exception e) {
            log.warn("Failed to send welcome email to {}: {}", savedUser.getEmail(), e.getMessage());
        }

        // Generate JWT token — returned to controller which sets it as HttpOnly cookie
        org.springframework.security.core.userdetails.User userDetails =
                new org.springframework.security.core.userdetails.User(
                        savedUser.getEmail(),
                        savedUser.getPassword(),
                        java.util.Collections.emptyList()
                );
        String token = jwtTokenProvider.generateToken(userDetails);

        AuthResponse authResponse = AuthResponse.builder()
                .user(mapToUserResponse(savedUser))
                .build();

        return new AuthResult(token, authResponse);
    }

    /**
     * Authenticates a user and returns the generated JWT token alongside the AuthResponse.
     * The controller is responsible for placing the token in an HttpOnly cookie.
     */
    public AuthResult login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtTokenProvider.generateToken(userDetails);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", null));

        log.info("User logged in: {}", user.getEmail());

        AuthResponse authResponse = AuthResponse.builder()
                .user(mapToUserResponse(user))
                .build();

        return new AuthResult(token, authResponse);
    }

    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return mapToUserResponse(user);
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .subscriptionStatus(user.getSubscriptionStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
