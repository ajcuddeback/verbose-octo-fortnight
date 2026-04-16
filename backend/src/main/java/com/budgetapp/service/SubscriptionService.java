package com.budgetapp.service;

import com.budgetapp.dto.response.SubscriptionResponse;
import com.budgetapp.exception.ResourceNotFoundException;
import com.budgetapp.model.Subscription;
import com.budgetapp.model.SubscriptionStatus;
import com.budgetapp.model.User;
import com.budgetapp.repository.SubscriptionRepository;
import com.budgetapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public SubscriptionResponse getSubscriptionStatus(Long userId) {
        User user = getUserById(userId);
        List<Subscription> subscriptions = subscriptionRepository.findByUserOrderByCreatedAtDesc(user);

        if (subscriptions.isEmpty()) {
            return SubscriptionResponse.builder()
                    .status(user.getSubscriptionStatus())
                    .build();
        }

        Subscription latest = subscriptions.get(0);
        return mapToResponse(latest);
    }

    @Transactional
    public SubscriptionResponse createCheckoutSession(Long userId) {
        User user = getUserById(userId);

        // TODO: Integrate Stripe API here
        // TODO: Replace this stub with actual Stripe implementation:
        //   Stripe.apiKey = stripeSecretKey; // from application.properties: stripe.api-key=sk_live_YOUR_KEY_HERE
        //   SessionCreateParams params = SessionCreateParams.builder()
        //     .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
        //     .setSuccessUrl(frontendUrl + "/subscription/success?session_id={CHECKOUT_SESSION_ID}")
        //     .setCancelUrl(frontendUrl + "/subscription/cancel")
        //     .addLineItem(SessionCreateParams.LineItem.builder()
        //         .setPrice("price_YOUR_STRIPE_PRICE_ID")
        //         .setQuantity(1L)
        //         .build())
        //     .setCustomerEmail(user.getEmail())
        //     .build();
        //   Session session = Session.create(params);
        //   user.setStripeCustomerId(session.getCustomer());
        //   return session.getUrl(); // redirect user here

        // Simulating payment success for now
        user.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        userRepository.save(user);

        Subscription subscription = Subscription.builder()
                .user(user)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1))
                .amountCents(500)
                // TODO: stripeSubscriptionId and stripePaymentIntentId will be set from Stripe webhook
                .build();

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("Subscription activated (stub) for userId: {}", userId);

        SubscriptionResponse response = mapToResponse(saved);
        response.setCheckoutUrl("http://localhost:4200/subscription/success"); // Mock URL
        return response;
    }

    @Transactional
    public SubscriptionResponse cancelSubscription(Long userId) {
        User user = getUserById(userId);
        List<Subscription> subscriptions = subscriptionRepository.findByUserOrderByCreatedAtDesc(user);

        if (subscriptions.isEmpty()) {
            throw new ResourceNotFoundException("No subscription found for user: " + userId);
        }

        Subscription activeSubscription = subscriptions.stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No active subscription to cancel"));

        // TODO: Call Stripe API to cancel subscription:
        //   Stripe.apiKey = stripeSecretKey;
        //   Subscription stripeSub = Subscription.retrieve(activeSubscription.getStripeSubscriptionId());
        //   stripeSub.cancel();

        activeSubscription.setStatus(SubscriptionStatus.CANCELLED);
        activeSubscription.setCancelledAt(LocalDateTime.now());
        subscriptionRepository.save(activeSubscription);

        user.setSubscriptionStatus(SubscriptionStatus.CANCELLED);
        // Set data wipe date to 2 years from cancellation
        LocalDate dataWipeDate = LocalDate.now().plusYears(2);
        user.setDataWipeScheduledDate(dataWipeDate);
        user.setDataWipeNotificationSent(false);
        userRepository.save(user);

        log.info("Subscription cancelled for userId: {}. Data wipe scheduled for: {}", userId, dataWipeDate);

        // Send cancellation email
        try {
            emailService.sendSubscriptionCancelledEmail(user, dataWipeDate);
        } catch (Exception e) {
            log.warn("Failed to send cancellation email to {}: {}", user.getEmail(), e.getMessage());
        }

        return mapToResponse(activeSubscription);
    }

    public void handleWebhook(String payload, String sigHeader) {
        // TODO: Stripe webhook signature verification:
        //   Stripe.apiKey = stripeSecretKey;
        //   Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        //   // Handle event types:
        //   // "customer.subscription.created" -> activate subscription
        //   // "customer.subscription.deleted" -> cancel subscription
        //   // "invoice.payment_succeeded" -> extend subscription end date
        //   // "invoice.payment_failed" -> mark subscription as expired
        //   // Add stripe.webhook-secret=whsec_YOUR_SECRET to application.properties

        log.info("Webhook received (stub) - payload length: {}", payload != null ? payload.length() : 0);
    }

    public boolean isSubscriptionActive(Long userId) {
        User user = getUserById(userId);
        return user.getSubscriptionStatus() == SubscriptionStatus.ACTIVE
                || user.getSubscriptionStatus() == SubscriptionStatus.FREE_TRIAL;
    }

    private SubscriptionResponse mapToResponse(Subscription subscription) {
        return SubscriptionResponse.builder()
                .status(subscription.getStatus())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .cancelledAt(subscription.getCancelledAt())
                .amountCents(subscription.getAmountCents())
                .build();
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}
