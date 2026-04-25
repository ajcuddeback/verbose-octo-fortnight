package com.agentflow.service;

import com.agentflow.dto.CreditBalanceResponse;
import com.agentflow.dto.CreditEstimateResponse;
import com.agentflow.dto.CreditTransactionResponse;
import com.agentflow.model.CreditBalance;
import com.agentflow.model.CreditTransaction;
import com.agentflow.model.Project;
import com.agentflow.model.Subscription;
import com.agentflow.model.User;
import com.agentflow.model.enums.ComplexityTier;
import com.agentflow.model.enums.TransactionType;
import com.agentflow.repository.CreditBalanceRepository;
import com.agentflow.repository.CreditTransactionRepository;
import com.agentflow.repository.SubscriptionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CreditService {

    private final CreditBalanceRepository creditBalanceRepository;
    private final CreditTransactionRepository transactionRepository;
    private final SubscriptionRepository subscriptionRepository;

    // -------------------------------------------------------------------------
    // Estimation — no auth required, safe to call publicly
    // -------------------------------------------------------------------------

    /**
     * Estimates the credit cost for a product idea.
     * TODO: Replace word-count heuristic with an LLM complexity analysis call.
     */
    public CreditEstimateResponse estimate(String productIdea) {
        int wordCount = productIdea.trim().split("\\s+").length;
        ComplexityTier tier = tierFromWordCount(wordCount);
        return CreditEstimateResponse.builder()
            .tier(tier)
            .creditCost(tier.getCreditCost())
            .summary(tier.getSummary())
            .rationale(tier.getRationale())
            .wordCount(wordCount)
            .build();
    }

    // -------------------------------------------------------------------------
    // Balance
    // -------------------------------------------------------------------------

    @Transactional
    public CreditBalanceResponse getBalance(User user) {
        CreditBalance balance = getOrCreateBalance(user);
        maybeResetMonthlyCredits(balance, user);

        Subscription sub = subscriptionRepository.findByUserId(user.getId()).orElse(null);
        LocalDate nextReset = balance.getLastResetDate() != null
            ? balance.getLastResetDate().plusMonths(1)
            : LocalDate.now().plusMonths(1);

        return CreditBalanceResponse.builder()
            .balance(balance.getBalance())
            .monthlyAllowance(balance.getMonthlyAllowance())
            .lastResetDate(balance.getLastResetDate())
            .nextResetDate(nextReset)
            .subscriptionPlan(sub != null ? sub.getPlan().name() : "FREE")
            .subscriptionStatus(sub != null ? sub.getStatus().name() : "ACTIVE")
            .build();
    }

    public List<CreditTransactionResponse> getTransactionHistory(User user) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
            .stream()
            .map(this::toTransactionResponse)
            .toList();
    }

    // -------------------------------------------------------------------------
    // Deduction / addition (used by ProjectService and SubscriptionService)
    // -------------------------------------------------------------------------

    @Transactional
    public void deductCredits(User user, int amount, Project project) {
        CreditBalance balance = getOrCreateBalance(user);
        maybeResetMonthlyCredits(balance, user);

        if (balance.getBalance() < amount) {
            throw new IllegalStateException(
                "Insufficient credits. Required: " + amount + ", available: " + balance.getBalance());
        }

        balance.setBalance(balance.getBalance() - amount);
        creditBalanceRepository.save(balance);

        transactionRepository.save(CreditTransaction.builder()
            .user(user)
            .amount(-amount)
            .balanceAfter(balance.getBalance())
            .type(TransactionType.WORKFLOW_DEDUCTION)
            .description("Workflow started for project: " + project.getName())
            .project(project)
            .build());
    }

    @Transactional
    public void refundCredits(User user, int amount, Project project) {
        CreditBalance balance = getOrCreateBalance(user);
        balance.setBalance(balance.getBalance() + amount);
        creditBalanceRepository.save(balance);

        transactionRepository.save(CreditTransaction.builder()
            .user(user)
            .amount(amount)
            .balanceAfter(balance.getBalance())
            .type(TransactionType.REFUND)
            .description("Refund for failed workflow: " + project.getName())
            .project(project)
            .build());
    }

    @Transactional
    public void addCredits(User user, int amount, TransactionType type, String description) {
        CreditBalance balance = getOrCreateBalance(user);
        balance.setBalance(balance.getBalance() + amount);
        creditBalanceRepository.save(balance);

        transactionRepository.save(CreditTransaction.builder()
            .user(user)
            .amount(amount)
            .balanceAfter(balance.getBalance())
            .type(type)
            .description(description)
            .build());
    }

    public boolean hasEnoughCredits(User user, int required) {
        return getOrCreateBalance(user).getBalance() >= required;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private CreditBalance getOrCreateBalance(User user) {
        return creditBalanceRepository.findByUserId(user.getId())
            .orElseThrow(() -> new EntityNotFoundException("Credit balance not found for user: " + user.getId()));
    }

    /** Resets monthly allowance if a new calendar month has started since the last reset. */
    private void maybeResetMonthlyCredits(CreditBalance balance, User user) {
        LocalDate today = LocalDate.now();
        LocalDate lastReset = balance.getLastResetDate();
        if (lastReset == null || today.getMonthValue() != lastReset.getMonthValue()
                || today.getYear() != lastReset.getYear()) {
            int allowance = balance.getMonthlyAllowance();
            balance.setBalance(balance.getBalance() + allowance);
            balance.setLastResetDate(today);
            creditBalanceRepository.save(balance);

            transactionRepository.save(CreditTransaction.builder()
                .user(user)
                .amount(allowance)
                .balanceAfter(balance.getBalance())
                .type(TransactionType.MONTHLY_GRANT)
                .description("Monthly credit grant (" + allowance + " credits)")
                .build());
        }
    }

    private ComplexityTier tierFromWordCount(int words) {
        if (words < 50)  return ComplexityTier.SIMPLE;
        if (words < 150) return ComplexityTier.MEDIUM;
        if (words < 300) return ComplexityTier.COMPLEX;
        return ComplexityTier.ENTERPRISE;
    }

    private CreditTransactionResponse toTransactionResponse(CreditTransaction t) {
        return CreditTransactionResponse.builder()
            .id(t.getId())
            .amount(t.getAmount())
            .balanceAfter(t.getBalanceAfter())
            .type(t.getType())
            .description(t.getDescription())
            .projectId(t.getProject() != null ? t.getProject().getId() : null)
            .createdAt(t.getCreatedAt())
            .build();
    }
}
