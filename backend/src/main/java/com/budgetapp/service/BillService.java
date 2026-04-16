package com.budgetapp.service;

import com.budgetapp.dto.request.BillRequest;
import com.budgetapp.dto.response.BillResponse;
import com.budgetapp.exception.ResourceNotFoundException;
import com.budgetapp.exception.UnauthorizedException;
import com.budgetapp.model.Bill;
import com.budgetapp.model.BillFrequency;
import com.budgetapp.model.Category;
import com.budgetapp.model.User;
import com.budgetapp.repository.BillRepository;
import com.budgetapp.repository.CategoryRepository;
import com.budgetapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillService {

    private final BillRepository billRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public BillResponse createBill(BillRequest request, Long userId) {
        User user = getUserById(userId);

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));
        }

        Bill bill = Bill.builder()
                .user(user)
                .name(request.getName())
                .amount(request.getAmount())
                .dueDay(request.getDueDay())
                .frequency(request.getFrequency())
                .category(category)
                .isAutoPay(request.isAutoPay())
                .isActive(request.isActive())
                .build();

        Bill saved = billRepository.save(bill);
        log.info("Bill created with id: {} for userId: {}", saved.getId(), userId);
        return mapToResponse(saved);
    }

    public List<BillResponse> getUserBills(Long userId) {
        User user = getUserById(userId);
        List<Bill> bills = billRepository.findByUserAndIsActive(user, true);
        return bills.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public BillResponse updateBill(Long id, BillRequest request, Long userId) {
        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill", id));

        if (!bill.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Bill does not belong to this user");
        }

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));
        }

        bill.setName(request.getName());
        bill.setAmount(request.getAmount());
        bill.setDueDay(request.getDueDay());
        bill.setFrequency(request.getFrequency());
        bill.setCategory(category);
        bill.setAutoPay(request.isAutoPay());
        bill.setActive(request.isActive());

        Bill updated = billRepository.save(bill);
        log.info("Bill updated with id: {} for userId: {}", id, userId);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteBill(Long id, Long userId) {
        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill", id));

        if (!bill.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Bill does not belong to this user");
        }

        billRepository.delete(bill);
        log.info("Bill deleted with id: {} for userId: {}", id, userId);
    }

    @Transactional
    public BillResponse markBillPaid(Long billId, Long userId) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill", billId));

        if (!bill.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Bill does not belong to this user");
        }

        bill.setLastPaidDate(LocalDate.now());
        Bill updated = billRepository.save(bill);
        log.info("Bill marked as paid: {} for userId: {}", billId, userId);
        return mapToResponse(updated);
    }

    private LocalDate calculateNextDueDate(Bill bill) {
        LocalDate today = LocalDate.now();
        LocalDate nextDue;

        // Start with current month's due date
        try {
            nextDue = LocalDate.of(today.getYear(), today.getMonth(), bill.getDueDay());
        } catch (Exception e) {
            // Handle months that don't have the specified day (e.g., Feb 31 -> Feb 28)
            nextDue = LocalDate.of(today.getYear(), today.getMonth(), 1)
                    .withDayOfMonth(Math.min(bill.getDueDay(),
                            today.getMonth().length(today.isLeapYear())));
        }

        // If the due date has passed this month, calculate next occurrence
        if (!nextDue.isAfter(today)) {
            switch (bill.getFrequency()) {
                case MONTHLY -> nextDue = nextDue.plusMonths(1);
                case QUARTERLY -> nextDue = nextDue.plusMonths(3);
                case ANNUALLY -> nextDue = nextDue.plusYears(1);
                case WEEKLY -> nextDue = nextDue.plusWeeks(1);
            }
        }

        return nextDue;
    }

    private BillResponse mapToResponse(Bill bill) {
        return BillResponse.builder()
                .id(bill.getId())
                .name(bill.getName())
                .amount(bill.getAmount())
                .dueDay(bill.getDueDay())
                .frequency(bill.getFrequency())
                .categoryId(bill.getCategory() != null ? bill.getCategory().getId() : null)
                .categoryName(bill.getCategory() != null ? bill.getCategory().getName() : null)
                .isAutoPay(bill.isAutoPay())
                .isActive(bill.isActive())
                .lastPaidDate(bill.getLastPaidDate())
                .nextDueDate(calculateNextDueDate(bill))
                .createdAt(bill.getCreatedAt())
                .build();
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}
