package com.budgetapp.scheduler;

import com.budgetapp.model.SubscriptionStatus;
import com.budgetapp.model.User;
import com.budgetapp.repository.*;
import com.budgetapp.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataRetentionScheduler {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final GoalRepository goalRepository;
    private final AccountRepository accountRepository;
    private final BillRepository billRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final EmailService emailService;

    /**
     * Runs daily at 9am.
     * For cancelled users:
     * - If 30 days before wipe date and notification not sent: send warning email
     * - If wipe date reached: delete all user data
     */
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void processDataRetention() {
        log.info("Starting daily data retention check...");

        List<User> cancelledUsers = userRepository.findAll().stream()
                .filter(user -> user.getSubscriptionStatus() == SubscriptionStatus.CANCELLED)
                .filter(user -> user.getDataWipeScheduledDate() != null)
                .toList();

        log.info("Found {} cancelled users to process", cancelledUsers.size());

        LocalDate today = LocalDate.now();

        for (User user : cancelledUsers) {
            try {
                processUserDataRetention(user, today);
            } catch (Exception e) {
                log.error("Error processing data retention for user {}: {}",
                        user.getId(), e.getMessage(), e);
            }
        }

        log.info("Daily data retention check complete.");
    }

    private void processUserDataRetention(User user, LocalDate today) {
        LocalDate wipeDate = user.getDataWipeScheduledDate();
        long daysUntilWipe = today.until(wipeDate).getDays();

        // Check if 30 days warning should be sent
        if (daysUntilWipe <= 30 && daysUntilWipe > 0 && !user.isDataWipeNotificationSent()) {
            log.info("Sending data deletion warning to user {} (wipe date: {})",
                    user.getEmail(), wipeDate);
            try {
                emailService.sendDataDeletionWarningEmail(user, wipeDate);
                user.setDataWipeNotificationSent(true);
                userRepository.save(user);
                log.info("Data deletion warning sent to user {}", user.getEmail());
            } catch (Exception e) {
                log.error("Failed to send deletion warning to user {}: {}",
                        user.getEmail(), e.getMessage());
            }
        }

        // Check if wipe date has been reached
        if (!today.isBefore(wipeDate)) {
            log.info("Wipe date reached for user {}. Deleting all user data...", user.getEmail());
            deleteAllUserData(user);
        }
    }

    private void deleteAllUserData(User user) {
        Long userId = user.getId();
        String email = user.getEmail();

        try {
            // Delete transactions
            List<com.budgetapp.model.Transaction> transactions =
                    transactionRepository.findByUserOrderByTransactionDateDesc(user);
            transactionRepository.deleteAll(transactions);
            log.info("Deleted {} transactions for user {}", transactions.size(), email);

            // Delete budgets
            List<com.budgetapp.model.Budget> budgets =
                    budgetRepository.findAll().stream()
                            .filter(b -> b.getUser().getId().equals(userId))
                            .toList();
            budgetRepository.deleteAll(budgets);
            log.info("Deleted {} budgets for user {}", budgets.size(), email);

            // Delete goals
            List<com.budgetapp.model.Goal> goals = goalRepository.findByUser(user);
            goalRepository.deleteAll(goals);
            log.info("Deleted {} goals for user {}", goals.size(), email);

            // Delete accounts
            List<com.budgetapp.model.Account> accounts = accountRepository.findByUser(user);
            accountRepository.deleteAll(accounts);
            log.info("Deleted {} accounts for user {}", accounts.size(), email);

            // Delete bills
            List<com.budgetapp.model.Bill> bills = billRepository.findAll().stream()
                    .filter(b -> b.getUser().getId().equals(userId))
                    .toList();
            billRepository.deleteAll(bills);
            log.info("Deleted {} bills for user {}", bills.size(), email);

            // Delete subscriptions
            List<com.budgetapp.model.Subscription> subscriptions =
                    subscriptionRepository.findByUserOrderByCreatedAtDesc(user);
            subscriptionRepository.deleteAll(subscriptions);
            log.info("Deleted {} subscriptions for user {}", subscriptions.size(), email);

            // Delete user
            userRepository.delete(user);
            log.info("User {} (id: {}) permanently deleted due to data retention policy", email, userId);

        } catch (Exception e) {
            log.error("Error deleting data for user {} (id: {}): {}", email, userId, e.getMessage(), e);
            throw e;
        }
    }
}
