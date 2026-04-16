package com.budgetapp.service;

import com.budgetapp.dto.response.FinancialHealthResponse;
import com.budgetapp.exception.ResourceNotFoundException;
import com.budgetapp.model.*;
import com.budgetapp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialHealthService {

    private final FinancialHealthScoreRepository healthScoreRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final GoalRepository goalRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public FinancialHealthResponse calculateAndSaveHealthScore(Long userId) {
        User user = getUserById(userId);
        YearMonth lastMonth = YearMonth.now().minusMonths(1);

        double savingsRateScore = calculateSavingsRateScore(user, lastMonth);
        double budgetAdherenceScore = calculateBudgetAdherenceScore(user, lastMonth);
        double goalProgressScore = calculateGoalProgressScore(user);
        double emergencyFundScore = calculateEmergencyFundScore(user);
        double consistencyScore = calculateConsistencyScore(user);

        double overallScore = savingsRateScore + budgetAdherenceScore + goalProgressScore
                + emergencyFundScore + consistencyScore;

        FinancialHealthScore score = FinancialHealthScore.builder()
                .user(user)
                .overallScore(Math.min(100.0, overallScore))
                .savingsRateScore(savingsRateScore)
                .budgetAdherenceScore(budgetAdherenceScore)
                .goalProgressScore(goalProgressScore)
                .emergencyFundScore(emergencyFundScore)
                .consistencyScore(consistencyScore)
                .build();

        FinancialHealthScore saved = healthScoreRepository.save(score);
        log.info("Financial health score calculated for userId: {} - Score: {}", userId, overallScore);
        return mapToResponse(saved);
    }

    public FinancialHealthResponse getLatestHealthScore(Long userId) {
        User user = getUserById(userId);

        Optional<FinancialHealthScore> latestScore =
                healthScoreRepository.findTopByUserOrderByCalculatedAtDesc(user);

        if (latestScore.isEmpty()) {
            // Auto-calculate if no score exists
            return calculateAndSaveHealthScore(userId);
        }

        return mapToResponse(latestScore.get());
    }

    public List<FinancialHealthResponse> getHealthScoreHistory(Long userId, int months) {
        User user = getUserById(userId);
        LocalDateTime startDate = LocalDateTime.now().minusMonths(months);
        LocalDateTime endDate = LocalDateTime.now();

        List<FinancialHealthScore> scores = healthScoreRepository
                .findByUserAndCalculatedAtBetween(user, startDate, endDate);

        return scores.stream()
                .sorted(Comparator.comparing(FinancialHealthScore::getCalculatedAt))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Savings rate score: (income - expenses) / income * 25 (capped 0-25)
     */
    private double calculateSavingsRateScore(User user, YearMonth month) {
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();

        List<Transaction> transactions = transactionRepository
                .findByUserAndTransactionDateBetween(user, startDate, endDate);

        BigDecimal income = transactions.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expenses = transactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (income.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }

        BigDecimal savingsRate = income.subtract(expenses)
                .divide(income, 4, RoundingMode.HALF_UP);

        double score = savingsRate.doubleValue() * 25.0;
        return Math.max(0.0, Math.min(25.0, score));
    }

    /**
     * Budget adherence score: % of budgets where spending <= allocated * 25
     */
    private double calculateBudgetAdherenceScore(User user, YearMonth month) {
        List<Budget> budgets = budgetRepository.findByUserAndMonthAndYear(
                user, month.getMonthValue(), month.getYear());

        if (budgets.isEmpty()) {
            return 0.0;
        }

        long adherentBudgets = budgets.stream()
                .filter(budget -> {
                    if (budget.getCategory() == null) return true;
                    LocalDate startDate = month.atDay(1);
                    LocalDate endDate = month.atEndOfMonth();

                    List<Transaction> transactions = transactionRepository
                            .findByUserAndCategoryIdAndDateBetween(user,
                                    budget.getCategory().getId(), startDate, endDate);

                    BigDecimal spent = transactions.stream()
                            .filter(t -> t.getType() == TransactionType.EXPENSE)
                            .map(Transaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return spent.compareTo(budget.getAllocatedAmount()) <= 0;
                })
                .count();

        double adherenceRate = (double) adherentBudgets / budgets.size();
        return adherenceRate * 25.0;
    }

    /**
     * Goal progress score: average goal completion % * 20
     */
    private double calculateGoalProgressScore(User user) {
        List<Goal> goals = goalRepository.findByUser(user);

        if (goals.isEmpty()) {
            return 0.0;
        }

        double avgProgress = goals.stream()
                .mapToDouble(goal -> {
                    if (goal.getTargetAmount() == null || goal.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
                        return 0.0;
                    }
                    double progress = goal.getCurrentAmount()
                            .divide(goal.getTargetAmount(), 4, RoundingMode.HALF_UP)
                            .doubleValue();
                    return Math.min(1.0, progress);
                })
                .average()
                .orElse(0.0);

        return avgProgress * 20.0;
    }

    /**
     * Emergency fund score: total savings / (3 * monthly_expenses) * 20 (capped 0-20)
     */
    private double calculateEmergencyFundScore(User user) {
        // Total savings account balance
        List<Account> savingsAccounts = accountRepository.findByUserAndIsActive(user, true)
                .stream()
                .filter(a -> a.getType() == AccountType.SAVINGS)
                .collect(Collectors.toList());

        BigDecimal totalSavings = savingsAccounts.stream()
                .map(Account::getBalance)
                .filter(b -> b != null && b.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Average monthly expenses over last 3 months
        BigDecimal monthlyExpenses = getAverageMonthlyExpenses(user, 3);

        if (monthlyExpenses.compareTo(BigDecimal.ZERO) <= 0) {
            return totalSavings.compareTo(BigDecimal.ZERO) > 0 ? 20.0 : 0.0;
        }

        BigDecimal emergencyFundTarget = monthlyExpenses.multiply(BigDecimal.valueOf(3));
        double ratio = totalSavings.divide(emergencyFundTarget, 4, RoundingMode.HALF_UP).doubleValue();
        return Math.min(20.0, ratio * 20.0);
    }

    /**
     * Consistency score: has data for last 3 months? 10 : (months_with_data/3)*10
     */
    private double calculateConsistencyScore(User user) {
        int monthsWithData = 0;
        YearMonth currentMonth = YearMonth.now();

        for (int i = 1; i <= 3; i++) {
            YearMonth targetMonth = currentMonth.minusMonths(i);
            LocalDate startDate = targetMonth.atDay(1);
            LocalDate endDate = targetMonth.atEndOfMonth();

            List<Transaction> transactions = transactionRepository
                    .findByUserAndTransactionDateBetween(user, startDate, endDate);

            if (!transactions.isEmpty()) {
                monthsWithData++;
            }
        }

        if (monthsWithData >= 3) {
            return 10.0;
        }
        return ((double) monthsWithData / 3.0) * 10.0;
    }

    private BigDecimal getAverageMonthlyExpenses(User user, int months) {
        YearMonth currentMonth = YearMonth.now();
        BigDecimal totalExpenses = BigDecimal.ZERO;

        for (int i = 1; i <= months; i++) {
            YearMonth targetMonth = currentMonth.minusMonths(i);
            LocalDate startDate = targetMonth.atDay(1);
            LocalDate endDate = targetMonth.atEndOfMonth();

            List<Transaction> monthTransactions = transactionRepository
                    .findByUserAndTransactionDateBetween(user, startDate, endDate);

            BigDecimal monthExpenses = monthTransactions.stream()
                    .filter(t -> t.getType() == TransactionType.EXPENSE)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalExpenses = totalExpenses.add(monthExpenses);
        }

        return totalExpenses.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
    }

    private String calculateGrade(double score) {
        if (score >= 90) return "A";
        if (score >= 75) return "B";
        if (score >= 60) return "C";
        if (score >= 45) return "D";
        return "F";
    }

    private List<String> generateInsights(FinancialHealthScore score) {
        List<String> insights = new ArrayList<>();
        double overall = score.getOverallScore();

        if (score.getSavingsRateScore() < 10) {
            insights.add("Your savings rate is low. Try to save at least 20% of your income each month.");
        } else if (score.getSavingsRateScore() >= 20) {
            insights.add("Excellent savings rate! You're saving a healthy portion of your income.");
        }

        if (score.getBudgetAdherenceScore() < 15) {
            insights.add("You're frequently exceeding your budget limits. Review and adjust your budgets.");
        } else if (score.getBudgetAdherenceScore() >= 22) {
            insights.add("Great budget discipline! You're consistently staying within your limits.");
        }

        if (score.getGoalProgressScore() < 8) {
            insights.add("Consider making regular contributions to your financial goals.");
        } else if (score.getGoalProgressScore() >= 15) {
            insights.add("You're making solid progress toward your financial goals. Keep it up!");
        }

        if (score.getEmergencyFundScore() < 10) {
            insights.add("Build your emergency fund to cover 3-6 months of expenses for financial security.");
        } else if (score.getEmergencyFundScore() >= 18) {
            insights.add("Excellent emergency fund! You're well-prepared for unexpected expenses.");
        }

        if (score.getConsistencyScore() < 7) {
            insights.add("Track your finances consistently every month for better insights.");
        }

        if (overall >= 90) {
            insights.add("Outstanding financial health! You're in excellent financial shape.");
        } else if (overall >= 75) {
            insights.add("Good financial health overall. Keep working on your weak areas.");
        } else if (overall < 45) {
            insights.add("Focus on the basics: reduce expenses, build savings, and stick to a budget.");
        }

        return insights;
    }

    private FinancialHealthResponse mapToResponse(FinancialHealthScore score) {
        Map<String, Double> breakdown = new LinkedHashMap<>();
        breakdown.put("savingsRate", score.getSavingsRateScore());
        breakdown.put("budgetAdherence", score.getBudgetAdherenceScore());
        breakdown.put("goalProgress", score.getGoalProgressScore());
        breakdown.put("emergencyFund", score.getEmergencyFundScore());
        breakdown.put("consistency", score.getConsistencyScore());

        return FinancialHealthResponse.builder()
                .id(score.getId())
                .overallScore(score.getOverallScore())
                .savingsRateScore(score.getSavingsRateScore())
                .budgetAdherenceScore(score.getBudgetAdherenceScore())
                .goalProgressScore(score.getGoalProgressScore())
                .emergencyFundScore(score.getEmergencyFundScore())
                .consistencyScore(score.getConsistencyScore())
                .calculatedAt(score.getCalculatedAt())
                .breakdown(breakdown)
                .grade(calculateGrade(score.getOverallScore()))
                .insights(generateInsights(score))
                .build();
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}
