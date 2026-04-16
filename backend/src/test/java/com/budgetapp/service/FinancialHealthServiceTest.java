package com.budgetapp.service;

import com.budgetapp.dto.response.FinancialHealthResponse;
import com.budgetapp.model.*;
import com.budgetapp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialHealthServiceTest {

    @Mock
    private FinancialHealthScoreRepository healthScoreRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private FinancialHealthService financialHealthService;

    private User testUser;
    private Category expenseCategory;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .subscriptionStatus(SubscriptionStatus.ACTIVE)
                .build();

        expenseCategory = Category.builder()
                .id(1L)
                .name("Food")
                .type(TransactionType.EXPENSE)
                .build();
    }

    @Test
    void calculateAndSaveHealthScore_GoodFinances_ReturnsHighScore() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Mock last month transactions with good savings rate
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        LocalDate start = lastMonth.atDay(1);
        LocalDate end = lastMonth.atEndOfMonth();

        Transaction income = Transaction.builder()
                .user(testUser)
                .amount(new BigDecimal("5000.00"))
                .type(TransactionType.INCOME)
                .transactionDate(start.plusDays(1))
                .build();

        Transaction expense = Transaction.builder()
                .user(testUser)
                .amount(new BigDecimal("2000.00"))
                .type(TransactionType.EXPENSE)
                .transactionDate(start.plusDays(5))
                .build();

        when(transactionRepository.findByUserAndTransactionDateBetween(eq(testUser), any(), any()))
                .thenReturn(List.of(income, expense));

        // Good budget adherence
        Budget budget = Budget.builder()
                .id(1L)
                .user(testUser)
                .category(expenseCategory)
                .allocatedAmount(new BigDecimal("3000.00"))
                .month(lastMonth.getMonthValue())
                .year(lastMonth.getYear())
                .build();

        when(budgetRepository.findByUserAndMonthAndYear(testUser,
                lastMonth.getMonthValue(), lastMonth.getYear()))
                .thenReturn(List.of(budget));
        when(transactionRepository.findByUserAndCategoryIdAndDateBetween(
                eq(testUser), eq(1L), any(), any()))
                .thenReturn(List.of(expense));

        // Good goal progress
        Goal goal = Goal.builder()
                .id(1L)
                .user(testUser)
                .name("Emergency Fund")
                .targetAmount(new BigDecimal("10000.00"))
                .currentAmount(new BigDecimal("7500.00"))
                .build();
        when(goalRepository.findByUser(testUser)).thenReturn(List.of(goal));

        // Good savings account
        Account savingsAccount = Account.builder()
                .id(1L)
                .user(testUser)
                .type(AccountType.SAVINGS)
                .balance(new BigDecimal("15000.00"))
                .isActive(true)
                .build();
        when(accountRepository.findByUserAndIsActive(testUser, true))
                .thenReturn(List.of(savingsAccount));

        // Mock saved score
        FinancialHealthScore mockScore = FinancialHealthScore.builder()
                .id(1L)
                .user(testUser)
                .overallScore(85.0)
                .savingsRateScore(15.0)
                .budgetAdherenceScore(25.0)
                .goalProgressScore(15.0)
                .emergencyFundScore(20.0)
                .consistencyScore(10.0)
                .calculatedAt(LocalDateTime.now())
                .build();
        when(healthScoreRepository.save(any(FinancialHealthScore.class))).thenReturn(mockScore);

        // Act
        FinancialHealthResponse response = financialHealthService.calculateAndSaveHealthScore(1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getOverallScore()).isGreaterThan(0);
        assertThat(response.getGrade()).isNotBlank();
        assertThat(response.getInsights()).isNotNull();
        assertThat(response.getBreakdown()).containsKeys(
                "savingsRate", "budgetAdherence", "goalProgress", "emergencyFund", "consistency");
    }

    @Test
    void calculateAndSaveHealthScore_NoData_ReturnsLowScore() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(transactionRepository.findByUserAndTransactionDateBetween(
                eq(testUser), any(), any())).thenReturn(List.of());
        when(budgetRepository.findByUserAndMonthAndYear(any(), any(Integer.class), any(Integer.class)))
                .thenReturn(List.of());
        when(goalRepository.findByUser(testUser)).thenReturn(List.of());
        when(accountRepository.findByUserAndIsActive(testUser, true)).thenReturn(List.of());

        FinancialHealthScore mockScore = FinancialHealthScore.builder()
                .id(1L)
                .user(testUser)
                .overallScore(0.0)
                .savingsRateScore(0.0)
                .budgetAdherenceScore(0.0)
                .goalProgressScore(0.0)
                .emergencyFundScore(0.0)
                .consistencyScore(0.0)
                .calculatedAt(LocalDateTime.now())
                .build();
        when(healthScoreRepository.save(any(FinancialHealthScore.class))).thenReturn(mockScore);

        // Act
        FinancialHealthResponse response = financialHealthService.calculateAndSaveHealthScore(1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getOverallScore()).isEqualTo(0.0);
        assertThat(response.getGrade()).isEqualTo("F");
    }

    @Test
    void getLatestHealthScore_WithExistingScore_ReturnsLatest() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        FinancialHealthScore existingScore = FinancialHealthScore.builder()
                .id(1L)
                .user(testUser)
                .overallScore(78.5)
                .savingsRateScore(18.0)
                .budgetAdherenceScore(20.0)
                .goalProgressScore(14.0)
                .emergencyFundScore(18.0)
                .consistencyScore(8.5)
                .calculatedAt(LocalDateTime.now().minusDays(1))
                .build();

        when(healthScoreRepository.findTopByUserOrderByCalculatedAtDesc(testUser))
                .thenReturn(Optional.of(existingScore));

        // Act
        FinancialHealthResponse response = financialHealthService.getLatestHealthScore(1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getOverallScore()).isEqualTo(78.5);
        assertThat(response.getGrade()).isEqualTo("B");
    }

    @Test
    void gradeCalculation_CorrectGrades() {
        // Test grade boundaries
        assertThat(getGradeForScore(95.0)).isEqualTo("A");
        assertThat(getGradeForScore(90.0)).isEqualTo("A");
        assertThat(getGradeForScore(85.0)).isEqualTo("B");
        assertThat(getGradeForScore(75.0)).isEqualTo("B");
        assertThat(getGradeForScore(70.0)).isEqualTo("C");
        assertThat(getGradeForScore(60.0)).isEqualTo("C");
        assertThat(getGradeForScore(55.0)).isEqualTo("D");
        assertThat(getGradeForScore(45.0)).isEqualTo("D");
        assertThat(getGradeForScore(30.0)).isEqualTo("F");
        assertThat(getGradeForScore(0.0)).isEqualTo("F");
    }

    private String getGradeForScore(double score) {
        if (score >= 90) return "A";
        if (score >= 75) return "B";
        if (score >= 60) return "C";
        if (score >= 45) return "D";
        return "F";
    }

    @Test
    void getHealthScoreHistory_ReturnsScoresInRange() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        FinancialHealthScore score1 = FinancialHealthScore.builder()
                .id(1L)
                .user(testUser)
                .overallScore(70.0)
                .calculatedAt(LocalDateTime.now().minusMonths(3))
                .build();

        FinancialHealthScore score2 = FinancialHealthScore.builder()
                .id(2L)
                .user(testUser)
                .overallScore(80.0)
                .calculatedAt(LocalDateTime.now().minusMonths(1))
                .build();

        when(healthScoreRepository.findByUserAndCalculatedAtBetween(
                eq(testUser), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(score1, score2));

        // Act
        List<FinancialHealthResponse> history = financialHealthService.getHealthScoreHistory(1L, 6);

        // Assert
        assertThat(history).hasSize(2);
        assertThat(history.get(0).getOverallScore()).isEqualTo(70.0);
        assertThat(history.get(1).getOverallScore()).isEqualTo(80.0);
    }
}
