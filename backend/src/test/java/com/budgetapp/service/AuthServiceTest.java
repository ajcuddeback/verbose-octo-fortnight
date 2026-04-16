package com.budgetapp.service;

import com.budgetapp.dto.request.LoginRequest;
import com.budgetapp.dto.request.RegisterRequest;
import com.budgetapp.dto.response.AuthResponse;
import com.budgetapp.model.Subscription;
import com.budgetapp.model.SubscriptionStatus;
import com.budgetapp.model.User;
import com.budgetapp.repository.SubscriptionRepository;
import com.budgetapp.repository.UserRepository;
import com.budgetapp.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("John");
        registerRequest.setLastName("Doe");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("encodedPassword")
                .firstName("John")
                .lastName("Doe")
                .subscriptionStatus(SubscriptionStatus.FREE_TRIAL)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void register_Success() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(new Subscription());
        when(jwtTokenProvider.generateToken(any(UserDetails.class))).thenReturn("mock-jwt-token");
        doNothing().when(emailService).sendWelcomeEmail(any(User.class));

        // Act
        AuthResponse response = authService.register(registerRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mock-jwt-token");
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getEmail()).isEqualTo("test@example.com");
        assertThat(response.getUser().getFirstName()).isEqualTo("John");

        verify(userRepository).save(any(User.class));
        verify(subscriptionRepository).save(any(Subscription.class));
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void register_DuplicateEmail_ThrowsException() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email is already registered");

        verify(userRepository, never()).save(any());
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void login_Success() {
        // Arrange
        Authentication mockAuth = mock(Authentication.class);
        UserDetails mockUserDetails = mock(UserDetails.class);

        when(mockUserDetails.getUsername()).thenReturn("test@example.com");
        when(mockUserDetails.getPassword()).thenReturn("encodedPassword");
        when(mockAuth.getPrincipal()).thenReturn(mockUserDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
        when(jwtTokenProvider.generateToken(any(UserDetails.class))).thenReturn("mock-jwt-token");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        // Act
        AuthResponse response = authService.login(loginRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mock-jwt-token");
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void login_BadPassword_ThrowsException() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtTokenProvider, never()).generateToken(any());
    }

    @Test
    void register_PasswordIsEncoded() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("bcryptEncodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser = User.builder()
                    .id(1L)
                    .email(savedUser.getEmail())
                    .password(savedUser.getPassword())
                    .firstName(savedUser.getFirstName())
                    .lastName(savedUser.getLastName())
                    .subscriptionStatus(SubscriptionStatus.FREE_TRIAL)
                    .createdAt(LocalDateTime.now())
                    .build();
            return savedUser;
        });
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(new Subscription());
        when(jwtTokenProvider.generateToken(any(UserDetails.class))).thenReturn("token");
        doNothing().when(emailService).sendWelcomeEmail(any(User.class));

        // Act
        authService.register(registerRequest);

        // Assert
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(argThat(user ->
                user.getPassword().equals("bcryptEncodedPassword")));
    }
}
