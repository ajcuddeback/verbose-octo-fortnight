package com.budgetapp.service;

import com.budgetapp.dto.request.GoalRequest;
import com.budgetapp.dto.response.GoalResponse;
import com.budgetapp.exception.ResourceNotFoundException;
import com.budgetapp.exception.UnauthorizedException;
import com.budgetapp.model.Goal;
import com.budgetapp.model.User;
import com.budgetapp.repository.GoalRepository;
import com.budgetapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepository;
    private final UserRepository userRepository;

    @Transactional
    public GoalResponse createGoal(GoalRequest request, Long userId) {
        User user = getUserById(userId);

        Goal goal = Goal.builder()
                .user(user)
                .name(request.getName())
                .description(request.getDescription())
                .targetAmount(request.getTargetAmount())
                .currentAmount(BigDecimal.ZERO)
                .targetDate(request.getTargetDate())
                .category(request.getCategory())
                .isCompleted(false)
                .build();

        Goal saved = goalRepository.save(goal);
        log.info("Goal created with id: {} for userId: {}", saved.getId(), userId);
        return mapToResponse(saved);
    }

    public List<GoalResponse> getUserGoals(Long userId) {
        User user = getUserById(userId);
        List<Goal> goals = goalRepository.findByUser(user);
        return goals.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public GoalResponse updateGoal(Long id, GoalRequest request, Long userId) {
        Goal goal = goalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", id));

        if (!goal.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Goal does not belong to this user");
        }

        goal.setName(request.getName());
        goal.setDescription(request.getDescription());
        goal.setTargetAmount(request.getTargetAmount());
        goal.setTargetDate(request.getTargetDate());
        goal.setCategory(request.getCategory());

        Goal updated = goalRepository.save(goal);
        log.info("Goal updated with id: {} for userId: {}", id, userId);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteGoal(Long id, Long userId) {
        Goal goal = goalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", id));

        if (!goal.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Goal does not belong to this user");
        }

        goalRepository.delete(goal);
        log.info("Goal deleted with id: {} for userId: {}", id, userId);
    }

    @Transactional
    public GoalResponse contributeToGoal(Long goalId, BigDecimal amount, Long userId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", goalId));

        if (!goal.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Goal does not belong to this user");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Contribution amount must be positive");
        }

        BigDecimal newAmount = goal.getCurrentAmount().add(amount);
        goal.setCurrentAmount(newAmount);

        // Mark as completed if target reached
        if (newAmount.compareTo(goal.getTargetAmount()) >= 0) {
            goal.setCompleted(true);
            log.info("Goal {} completed for userId: {}", goalId, userId);
        }

        Goal updated = goalRepository.save(goal);
        log.info("Contributed {} to goal {} for userId: {}", amount, goalId, userId);
        return mapToResponse(updated);
    }

    private GoalResponse mapToResponse(Goal goal) {
        double progressPercentage = 0.0;
        if (goal.getTargetAmount() != null && goal.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
            progressPercentage = goal.getCurrentAmount()
                    .divide(goal.getTargetAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
            progressPercentage = Math.min(progressPercentage, 100.0);
        }

        LocalDate projectedCompletionDate = calculateProjectedCompletionDate(goal);

        return GoalResponse.builder()
                .id(goal.getId())
                .name(goal.getName())
                .description(goal.getDescription())
                .targetAmount(goal.getTargetAmount())
                .currentAmount(goal.getCurrentAmount())
                .targetDate(goal.getTargetDate())
                .category(goal.getCategory())
                .isCompleted(goal.isCompleted())
                .createdAt(goal.getCreatedAt())
                .progressPercentage(progressPercentage)
                .projectedCompletionDate(projectedCompletionDate)
                .build();
    }

    private LocalDate calculateProjectedCompletionDate(Goal goal) {
        if (goal.isCompleted()) {
            return LocalDate.now();
        }

        if (goal.getCreatedAt() == null || goal.getCurrentAmount().compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        // Calculate days since creation and average daily savings rate
        long daysSinceCreation = ChronoUnit.DAYS.between(goal.getCreatedAt().toLocalDate(), LocalDate.now());
        if (daysSinceCreation <= 0) {
            return null;
        }

        BigDecimal remaining = goal.getTargetAmount().subtract(goal.getCurrentAmount());
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            return LocalDate.now();
        }

        // Daily rate of contribution
        BigDecimal dailyRate = goal.getCurrentAmount()
                .divide(BigDecimal.valueOf(daysSinceCreation), 4, RoundingMode.HALF_UP);

        if (dailyRate.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        long daysToCompletion = remaining.divide(dailyRate, 0, RoundingMode.CEILING).longValue();
        return LocalDate.now().plusDays(daysToCompletion);
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}
