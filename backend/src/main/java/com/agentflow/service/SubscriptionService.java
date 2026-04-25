package com.agentflow.service;

import com.agentflow.config.CreditProperties;
import com.agentflow.config.StripeProperties;
import com.agentflow.dto.CheckoutResponse;
import com.agentflow.dto.SubscriptionResponse;
import com.agentflow.model.CreditBalance;
import com.agentflow.model.Subscription;
import com.agentflow.model.User;
import com.agentflow.model.enums.SubscriptionPlan;
import com.agentflow.model.enums.SubscriptionStatus;
import com.agentflow.model.enums.TransactionType;
import com.agentflow.repository.CreditBalanceRepository;
import com.agentflow.repository.SubscriptionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final CreditBalanceRepository creditBalanceRepository;
    private final CreditService creditService;
    private final StripeProperties stripeProperties;
    private final CreditProperties creditProperties;

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    public SubscriptionResponse getSubscription(User user) {
        Subscription sub = getOrThrow(user);
        return toResponse(sub);
    }

    // -------------------------------------------------------------------------
    // Checkout — redirect user to Stripe hosted page
    // -------------------------------------------------------------------------

    /**
     * Creates a Stripe Checkout session for a subscription upgrade.
     *
     * TODO: Integrate Stripe SDK
     *   1. Add dependency: com.stripe:stripe-java:25.x.x
     *   2. Stripe.apiKey = stripeProperties.getSecretKey();
     *   3. SessionCreateParams params = SessionCreateParams.builder()
     *        .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
     *        .setCustomerEmail(user.getEmail())
     *        .addLineItem(SessionCreateParams.LineItem.builder()
     *            .setPrice(priceIdForPlan(plan))
     *            .setQuantity(1L)
     *            .build())
     *        .setSuccessUrl("https://yourapp.com/subscription/success?session_id={CHECKOUT_SESSION_ID}")
     *        .setCancelUrl("https://yourapp.com/subscription/cancel")
     *        .putMetadata("userId", user.getId().toString())
     *        .build();
     *   4. Session session = Session.create(params);
     *   5. return CheckoutResponse.builder()
     *        .checkoutUrl(session.getUrl())
     *        .sessionId(session.getId())
     *        .build();
     */
    public CheckoutResponse createCheckoutSession(User user, SubscriptionPlan plan) {
        if (plan == SubscriptionPlan.FREE) {
            throw new IllegalArgumentException("Cannot checkout FREE plan");
        }
        log.info("TODO: Create Stripe checkout session for user {} plan {}", user.getId(), plan);
        return CheckoutResponse.builder()
            .checkoutUrl("https://TODO-stripe-checkout-url.com")
            .sessionId("cs_TODO")
            .build();
    }

    /**
     * Creates a Stripe Checkout session for a one-time credit pack purchase.
     *
     * TODO: Same as above but with Mode.PAYMENT and the appropriate pack price ID.
     *   Pack options: PACK_100 ($4.99/100cr), PACK_500 ($19.99/500cr), PACK_1000 ($34.99/1000cr)
     */
    public CheckoutResponse createCreditPackCheckout(User user, String packKey) {
        int credits = switch (packKey) {
            case "PACK_100"  -> 100;
            case "PACK_500"  -> 500;
            case "PACK_1000" -> 1000;
            default -> throw new IllegalArgumentException("Unknown credit pack: " + packKey);
        };
        log.info("TODO: Create Stripe payment session for user {} pack {} ({} credits)", user.getId(), packKey, credits);
        return CheckoutResponse.builder()
            .checkoutUrl("https://TODO-stripe-checkout-url.com")
            .sessionId("cs_TODO")
            .build();
    }

    // -------------------------------------------------------------------------
    // Webhook handler — called by StripeWebhookController
    // -------------------------------------------------------------------------

    /**
     * Processes verified Stripe webhook events.
     *
     * TODO: Implement full event handling:
     *   - checkout.session.completed  → upgrade subscription, update credit allowance
     *   - invoice.payment_succeeded   → renew period dates
     *   - invoice.payment_failed      → mark PAST_DUE
     *   - customer.subscription.deleted → cancel, downgrade to FREE
     *
     * Security: NEVER trust the raw payload — always verify the Stripe-Signature header first:
     *   Webhook.constructEvent(payload, sigHeader, stripeProperties.getWebhookSecret())
     */
    @Transactional
    public void handleWebhookEvent(String eventType, String stripeSubscriptionId,
                                   String stripeCustomerId, String userId, String planName) {
        log.info("TODO: Handle Stripe event '{}' for subscription {}", eventType, stripeSubscriptionId);

        switch (eventType) {
            case "checkout.session.completed" -> {
                // TODO: lookup user by userId metadata, upgrade their subscription and credit allowance
            }
            case "invoice.payment_succeeded" -> {
                // TODO: extend currentPeriodEnd
            }
            case "invoice.payment_failed" -> {
                // TODO: mark subscription PAST_DUE
            }
            case "customer.subscription.deleted" -> {
                // TODO: downgrade to FREE plan, reduce monthly allowance
            }
            default -> log.debug("Unhandled Stripe event type: {}", eventType);
        }
    }

    // -------------------------------------------------------------------------
    // Cancellation
    // -------------------------------------------------------------------------

    /**
     * Cancels the user's Stripe subscription at period end.
     *
     * TODO: Call stripe-java:
     *   Subscription stripeSub = Subscription.retrieve(sub.getStripeSubscriptionId());
     *   SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
     *       .setCancelAtPeriodEnd(true)
     *       .build();
     *   stripeSub.update(params);
     */
    @Transactional
    public SubscriptionResponse cancelSubscription(User user) {
        Subscription sub = getOrThrow(user);
        sub.setStatus(SubscriptionStatus.CANCELLED);
        subscriptionRepository.save(sub);
        log.info("TODO: Cancel Stripe subscription {} for user {}", sub.getStripeSubscriptionId(), user.getId());
        return toResponse(sub);
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    @Transactional
    public void upgradePlan(User user, SubscriptionPlan newPlan, String stripeCustomerId,
                             String stripeSubscriptionId) {
        Subscription sub = getOrThrow(user);
        sub.setPlan(newPlan);
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setStripeCustomerId(stripeCustomerId);
        sub.setStripeSubscriptionId(stripeSubscriptionId);
        subscriptionRepository.save(sub);

        int newAllowance = newPlan.getMonthlyCredits();
        CreditBalance balance = creditBalanceRepository.findByUserId(user.getId())
            .orElseThrow(() -> new EntityNotFoundException("No credit balance for user: " + user.getId()));
        int diff = newAllowance - balance.getMonthlyAllowance();
        balance.setMonthlyAllowance(newAllowance);
        creditBalanceRepository.save(balance);

        if (diff > 0) {
            creditService.addCredits(user, diff, TransactionType.PURCHASE,
                "Upgrade to " + newPlan.getDisplayName() + " — prorated credit grant");
        }
    }

    private Subscription getOrThrow(User user) {
        return subscriptionRepository.findByUserId(user.getId())
            .orElseThrow(() -> new EntityNotFoundException("Subscription not found for user: " + user.getId()));
    }

    private SubscriptionResponse toResponse(Subscription sub) {
        return SubscriptionResponse.builder()
            .plan(sub.getPlan())
            .planDisplayName(sub.getPlan().getDisplayName())
            .monthlyPrice(sub.getPlan().getMonthlyPrice())
            .monthlyCredits(sub.getPlan().getMonthlyCredits())
            .status(sub.getStatus())
            .currentPeriodEnd(sub.getCurrentPeriodEnd())
            .build();
    }
}
