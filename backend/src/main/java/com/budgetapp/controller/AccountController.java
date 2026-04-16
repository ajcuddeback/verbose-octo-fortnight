package com.budgetapp.controller;

import com.budgetapp.dto.request.AccountRequest;
import com.budgetapp.dto.response.AccountResponse;
import com.budgetapp.exception.ResourceNotFoundException;
import com.budgetapp.repository.UserRepository;
import com.budgetapp.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getUserAccounts(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserIdFromEmail(userDetails.getUsername());
        return ResponseEntity.ok(accountService.getUserAccounts(userId));
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AccountRequest request) {
        Long userId = getUserIdFromEmail(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccount(request, userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountResponse> updateAccount(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody AccountRequest request) {
        Long userId = getUserIdFromEmail(userDetails.getUsername());
        return ResponseEntity.ok(accountService.updateAccount(id, request, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        Long userId = getUserIdFromEmail(userDetails.getUsername());
        accountService.deleteAccount(id, userId);
        return ResponseEntity.noContent().build();
    }

    private Long getUserIdFromEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email))
                .getId();
    }
}
