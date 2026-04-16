package com.budgetapp.service;

import com.budgetapp.dto.response.SubscriptionResponse;
import com.budgetapp.exception.ResourceNotFoundException;
import com.budgetapp.model.Subscription;
import com.budgetapp.model.SubscriptionStatus;
import com.budgetapp.model.User;
import com.budgetapp.repository.SubscriptionRepository;
import com.budgetapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private User testUser;
    private Subscription activeSubscription;
    private Subscription trialSubscription;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .subscriptionStatus(SubscriptionStatus.FREE_TRIAL)
                .build();

        trialSubscription = Subscription.builder()
                .id(1L)
                .user(testUser)
                .status(SubscriptionStatus.FREE_TRIAL)
                .startDate(LocalDate.now().minusDays(7))
                .amountCents(0)
                .createdAt(LocalDateTime.now().minusDays(7))
                .build();

        activeSubscription = Subscription.builder()
                .id(2L)
                .user(testUser)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusMonths(1))
                .amountCents(500)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    @Test
    void getSubscriptionStatus_WithSubscription_ReturnsLatest() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(subscriptionRepository.findByUserOrderByCreatedAtDesc(testUser))
                .thenReturn(List.of(activeSubscription, trialSubscription));

        // Act
        SubscriptionResponse response = subscriptionService.getSubscriptionStatus(1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(response.getAmountCents()).isEqualTo(500);
    }

    @Test
    void getSubscriptionStatus_NoSubscription_ReturnsUserStatus() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(subscriptionRepository.findByUserOrderByCreatedAtDesc(testUser))
                .thenReturn(List.of());

        // Act
        SubscriptionResponse response = subscriptionService.getSubscriptionStatus(1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.FREE_TRIAL);
    }

    @Test
    void createCheckoutSession_Success() {
        // Arrange
        testUser.setSubscriptionStatus(SubscriptionStatus.FREE_TRIAL);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(activeSubscription);

        // Act
        SubscriptionResponse response = subscriptionService.createCheckoutSession(1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(response.getAmountCents()).isEqualTo(500);

        verify(userRepository).save(argThat(user ->
                user.getSubscriptionStatus() == SubscriptionStatus.ACTIVE));
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    void cancelSubscription_Success() {
        // Arrange
        testUser.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(subscriptionRepository.findByUserOrderByCreatedAtDesc(testUser))
                .thenReturn(List.of(activeSubscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(activeSubscription);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        doNothing().when(emailService).sendSubscriptionCancelledEmail(any(User.class), any(LocalDate.class));

        // Act
        SubscriptionResponse response = subscriptionService.cancelSubscription(1L);

        // Assert
        assertThat(response).isNotNull();

        // Verify user was updated with CANCELLED status and data wipe date
        verify(userRepository).save(argThat(user ->
                user.getSubscriptionStatus() == SubscriptionStatus.CANCELLED &&
                user.getDataWipeScheduledDate() != null &&
                user.getDataWipeScheduledDate().equals(LocalDate.now().plusYears(2))
        ));

        verify(subscriptionRepository).save(argThat(sub ->
                sub.getStatus() == SubscriptionStatus.CANCELLED &&
                sub.getCancelledAt() != null
        ));
    }

    @Test
    void cancelSubscription_NoActiveSubscription_ThrowsException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(subscriptionRepository.findByUserOrderByCreatedAtDesc(testUser))
                .thenReturn(List.of(trialSubscription));

        // Act & Assert
        assertThatThrownBy(() -> subscriptionService.cancelSubscription(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No active subscription to cancel");
    }

    @Test
    void cancelSubscription_NoSubscriptions_ThrowsResourceNotFoundException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(subscriptionRepository.findByUserOrderByCreatedAtDesc(testUser))
                .thenReturn(List.of());

        // Act & Assert
        assertThatThrownBy(() -> subscriptionService.cancelSubscription(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void isSubscriptionActive_ActiveUser_ReturnsTrue() {
        // Arrange
        testUser.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        boolean result = subscriptionService.isSubscriptionActive(1L);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void isSubscriptionActive_CancelledUser_ReturnsFalse() {
        // Arrange
        testUser.setSubscriptionStatus(SubscriptionStatus.CANCELLED);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        boolean result = subscriptionService.isSubscriptionActive(1L);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void isSubscriptionActive_FreeTrialUser_ReturnsTrue() {
        // Arrange
        testUser.setSubscriptionStatus(SubscriptionStatus.FREE_TRIAL);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        boolean result = subscriptionService.isSubscriptionActive(1L);

        // Assert
        assertThat(result).isTrue();
    }
}
