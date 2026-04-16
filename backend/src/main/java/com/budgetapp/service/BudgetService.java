package com.budgetapp.service;

import com.budgetapp.dto.request.BudgetRequest;
import com.budgetapp.dto.response.BudgetResponse;
import com.budgetapp.exception.ResourceNotFoundException;
import com.budgetapp.exception.UnauthorizedException;
import com.budgetapp.model.*;
import com.budgetapp.repository.BudgetRepository;
import com.budgetapp.repository.CategoryRepository;
import com.budgetapp.repository.TransactionRepository;
import com.budgetapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public BudgetResponse createBudget(BudgetRequest request, Long userId) {
        User user = getUserById(userId);
        Category category = getCategoryById(request.getCategoryId());

        Budget budget = Budget.builder()
                .user(user)
                .category(category)
                .allocatedAmount(request.getAllocatedAmount())
                .month(request.getMonth())
                .year(request.getYear())
                .build();

        Budget saved = budgetRepository.save(budget);
        log.info("Budget created with id: {} for userId: {}", saved.getId(), userId);
        return mapToResponse(saved, user);
    }

    public List<BudgetResponse> getBudgetsForMonth(Long userId, int month, int year) {
        User user = getUserById(userId);
        List<Budget> budgets = budgetRepository.findByUserAndMonthAndYear(user, month, year);

        return budgets.stream()
                .map(budget -> mapToResponse(budget, user))
                .collect(Collectors.toList());
    }

    @Transactional
    public BudgetResponse updateBudget(Long id, BudgetRequest request, Long userId) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget", id));

        if (!budget.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Budget does not belong to this user");
        }

        Category category = getCategoryById(request.getCategoryId());
        User user = getUserById(userId);

        budget.setCategory(category);
        budget.setAllocatedAmount(request.getAllocatedAmount());
        budget.setMonth(request.getMonth());
        budget.setYear(request.getYear());

        Budget updated = budgetRepository.save(budget);
        log.info("Budget updated with id: {} for userId: {}", id, userId);
        return mapToResponse(updated, user);
    }

    @Transactional
    public void deleteBudget(Long id, Long userId) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget", id));

        if (!budget.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Budget does not belong to this user");
        }

        budgetRepository.delete(budget);
        log.info("Budget deleted with id: {} for userId: {}", id, userId);
    }

    private BigDecimal computeSpentAmount(Budget budget, User user) {
        // Calculate first and last day of the budget's month
        LocalDate startDate = LocalDate.of(budget.getYear(), budget.getMonth(), 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        if (budget.getCategory() == null) {
            return BigDecimal.ZERO;
        }

        List<Transaction> transactions = transactionRepository.findByUserAndCategoryIdAndDateBetween(
                user, budget.getCategory().getId(), startDate, endDate);

        return transactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BudgetResponse mapToResponse(Budget budget, User user) {
        BigDecimal spentAmount = computeSpentAmount(budget, user);
        double percentage = 0.0;
        if (budget.getAllocatedAmount() != null && budget.getAllocatedAmount().compareTo(BigDecimal.ZERO) > 0) {
            percentage = spentAmount.divide(budget.getAllocatedAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        return BudgetResponse.builder()
                .id(budget.getId())
                .categoryId(budget.getCategory() != null ? budget.getCategory().getId() : null)
                .categoryName(budget.getCategory() != null ? budget.getCategory().getName() : null)
                .allocatedAmount(budget.getAllocatedAmount())
                .spentAmount(spentAmount)
                .month(budget.getMonth())
                .year(budget.getYear())
                .percentage(percentage)
                .build();
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private Category getCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
    }
}
