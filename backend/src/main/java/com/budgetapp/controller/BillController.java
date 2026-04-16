package com.budgetapp.controller;

import com.budgetapp.dto.request.BillRequest;
import com.budgetapp.dto.response.BillResponse;
import com.budgetapp.exception.ResourceNotFoundException;
import com.budgetapp.repository.UserRepository;
import com.budgetapp.service.BillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
public class BillController {

    private final BillService billService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<BillResponse>> getUserBills(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserIdFromEmail(userDetails.getUsername());
        return ResponseEntity.ok(billService.getUserBills(userId));
    }

    @PostMapping
    public ResponseEntity<BillResponse> createBill(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody BillRequest request) {
        Long userId = getUserIdFromEmail(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(billService.createBill(request, userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BillResponse> updateBill(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody BillRequest request) {
        Long userId = getUserIdFromEmail(userDetails.getUsername());
        return ResponseEntity.ok(billService.updateBill(id, request, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBill(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        Long userId = getUserIdFromEmail(userDetails.getUsername());
        billService.deleteBill(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/paid")
    public ResponseEntity<BillResponse> markBillPaid(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        Long userId = getUserIdFromEmail(userDetails.getUsername());
        return ResponseEntity.ok(billService.markBillPaid(id, userId));
    }

    private Long getUserIdFromEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email))
                .getId();
    }
}
