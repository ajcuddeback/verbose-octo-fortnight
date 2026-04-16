package com.budgetapp.controller;

import com.budgetapp.dto.request.GoalRequest;
import com.budgetapp.dto.response.GoalResponse;
import com.budgetapp.exception.ResourceNotFoundException;
import com.budgetapp.repository.UserRepository;
import com.budgetapp.service.GoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<GoalResponse>> getUserGoals(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserIdFromEmail(userDetails.getUsername());
        return ResponseEntity.ok(goalService.getUserGoals(userId));
    }

    @PostMapping
    public ResponseEntity<GoalResponse> createGoal(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody GoalRequest request) {
        Long userId = getUserIdFromEmail(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(goalService.createGoal(request, userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GoalResponse> updateGoal(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody GoalRequest request) {
        Long userId = getUserIdFromEmail(userDetails.getUsername());
        return ResponseEntity.ok(goalService.updateGoal(id, request, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoal(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        Long userId = getUserIdFromEmail(userDetails.getUsername());
        goalService.deleteGoal(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/contribute")
    public ResponseEntity<GoalResponse> contributeToGoal(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody Map<String, BigDecimal> body) {
        Long userId = getUserIdFromEmail(userDetails.getUsername());
        BigDecimal amount = body.get("amount");
        if (amount == null) {
            throw new IllegalArgumentException("amount field is required");
        }
        return ResponseEntity.ok(goalService.contributeToGoal(id, amount, userId));
    }

    private Long getUserIdFromEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email))
                .getId();
    }
}
