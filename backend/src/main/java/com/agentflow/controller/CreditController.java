package com.agentflow.controller;

import com.agentflow.dto.CreditBalanceResponse;
import com.agentflow.dto.CreditEstimateRequest;
import com.agentflow.dto.CreditEstimateResponse;
import com.agentflow.dto.CreditTransactionResponse;
import com.agentflow.model.User;
import com.agentflow.repository.UserRepository;
import com.agentflow.service.CreditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/credits")
@RequiredArgsConstructor
public class CreditController {

    private final CreditService creditService;
    private final UserRepository userRepository;

    /**
     * Public endpoint — returns cost estimate without requiring login.
     * Lets prospective users see how many credits their idea will use before signing up.
     */
    @PostMapping("/estimate")
    public CreditEstimateResponse estimate(@Valid @RequestBody CreditEstimateRequest request) {
        return creditService.estimate(request.getProductIdea());
    }

    @GetMapping("/balance")
    public CreditBalanceResponse balance(@AuthenticationPrincipal UserDetails principal) {
        return creditService.getBalance(resolveUser(principal));
    }

    @GetMapping("/transactions")
    public List<CreditTransactionResponse> transactions(@AuthenticationPrincipal UserDetails principal) {
        return creditService.getTransactionHistory(resolveUser(principal));
    }

    private User resolveUser(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername())
            .orElseThrow(() -> new IllegalStateException("User not found: " + principal.getUsername()));
    }
}
