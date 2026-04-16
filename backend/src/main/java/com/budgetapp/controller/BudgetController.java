package com.budgetapp.controller;

import com.budgetapp.dto.request.BudgetRequest;
import com.budgetapp.dto.response.BudgetResponse;
import com.budgetapp.exception.ResourceNotFoundException;
import com.budgetapp.repository.UserRepository;
import com.budgetapp.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<BudgetResponse>> getBudgetsForMonth(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().monthValue}") int month,
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().year}") int year) {
        Long userId = getUserIdFromEmail(userDetails.getUsername());
        // Default to current month/year if not provided
        if (month == 0) month = LocalDate.now().getMonthValue();
        if (year == 0) year = LocalDate.now().getYear();
        return ResponseEntity.ok(budgetService.getBudgetsForMonth(userId, month, year));
    }

    @PostMapping
    public ResponseEntity<BudgetResponse> createBudget(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody BudgetRequest request) {
        Long userId = getUserIdFromEmail(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(budgetService.createBudget(request, userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BudgetResponse> updateBudget(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody BudgetRequest request) {
        Long userId = getUserIdFromEmail(userDetails.getUsername());
        return ResponseEntity.ok(budgetService.updateBudget(id, request, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        Long userId = getUserIdFromEmail(userDetails.getUsername());
        budgetService.deleteBudget(id, userId);
        return ResponseEntity.noContent().build();
    }

    private Long getUserIdFromEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email))
                .getId();
    }
}
