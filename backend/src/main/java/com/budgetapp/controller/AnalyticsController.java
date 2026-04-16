package com.budgetapp.controller;

import com.budgetapp.dto.response.*;
import com.budgetapp.exception.ResourceNotFoundException;
import com.budgetapp.repository.UserRepository;
import com.budgetapp.service.AnalyticsService;
import com.budgetapp.service.FinancialHealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final FinancialHealthService financialHealthService;
    private final UserRepository userRepository;

    @GetMapping("/spending-trends")
    public ResponseEntity<SpendingTrendResponse> getSpendingTrends(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "6") int months) {
        Long userId = getUserIdFromEmail(userDetails.getUsername());
        return ResponseEntity.ok(analyticsService.getSpendingTrends(userId, months));
    }

    @GetMapping("/category-breakdown")
    public ResponseEntity<CategoryBreakdownResponse> getCategoryBreakdown(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        Long userId = getUserIdFromEmail(userDetails.getUsername());
        int targetMonth = month != null ? month : LocalDate.now().getMonthValue();
        int targetYear = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(analyticsService.getCategoryBreakdown(userId, targetMonth, targetYear));
    }

    @GetMapping("/predictions")
    public ResponseEntity<PredictionResponse> getPredictions(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserIdFromEmail(userDetails.getUsername());
        return ResponseEntity.ok(analyticsService.getPredictions(userId));
    }

    @GetMapping("/spending-dna")
    public ResponseEntity<SpendingDnaResponse> getSpendingDna(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserIdFromEmail(userDetails.getUsername());
        return ResponseEntity.ok(analyticsService.getSpendingDna(userId));
    }

    @GetMapping("/financial-health")
    public ResponseEntity<FinancialHealthResponse> getFinancialHealth(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserIdFromEmail(userDetails.getUsername());
        return ResponseEntity.ok(financialHealthService.getLatestHealthScore(userId));
    }

    @GetMapping("/financial-health/history")
    public ResponseEntity<List<FinancialHealthResponse>> getFinancialHealthHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "6") int months) {
        Long userId = getUserIdFromEmail(userDetails.getUsername());
        return ResponseEntity.ok(financialHealthService.getHealthScoreHistory(userId, months));
    }

    private Long getUserIdFromEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email))
                .getId();
    }
}
