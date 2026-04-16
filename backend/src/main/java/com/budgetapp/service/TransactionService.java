package com.budgetapp.service;

import com.budgetapp.dto.request.TransactionRequest;
import com.budgetapp.dto.response.TransactionResponse;
import com.budgetapp.exception.ResourceNotFoundException;
import com.budgetapp.exception.UnauthorizedException;
import com.budgetapp.model.*;
import com.budgetapp.repository.AccountRepository;
import com.budgetapp.repository.CategoryRepository;
import com.budgetapp.repository.TransactionRepository;
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
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request, Long userId) {
        User user = getUserById(userId);

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));
        }

        Account account = null;
        if (request.getAccountId() != null) {
            account = accountRepository.findById(request.getAccountId())
                    .orElseThrow(() -> new ResourceNotFoundException("Account", request.getAccountId()));
            // Verify account belongs to user
            if (!account.getUser().getId().equals(userId)) {
                throw new UnauthorizedException("Account does not belong to this user");
            }
        }

        Transaction transaction = Transaction.builder()
                .user(user)
                .amount(request.getAmount())
                .description(request.getDescription())
                .transactionDate(request.getTransactionDate())
                .type(request.getType())
                .category(category)
                .account(account)
                .notes(request.getNotes())
                .tags(request.getTags())
                .build();

        Transaction saved = transactionRepository.save(transaction);
        log.info("Transaction created with id: {} for userId: {}", saved.getId(), userId);
        return mapToResponse(saved);
    }

    public List<TransactionResponse> getUserTransactions(Long userId, LocalDate from, LocalDate to,
                                                          TransactionType type) {
        User user = getUserById(userId);

        List<Transaction> transactions;
        if (from != null && to != null && type != null) {
            transactions = transactionRepository.findByUserAndTypeAndTransactionDateBetween(user, type, from, to);
        } else if (from != null && to != null) {
            transactions = transactionRepository.findByUserAndTransactionDateBetween(user, from, to);
        } else if (type != null) {
            transactions = transactionRepository.findByUserAndType(user, type);
        } else {
            transactions = transactionRepository.findByUserOrderByTransactionDateDesc(user);
        }

        return transactions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TransactionResponse updateTransaction(Long transactionId, TransactionRequest request, Long userId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", transactionId));

        if (!transaction.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Transaction does not belong to this user");
        }

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));
        }

        Account account = null;
        if (request.getAccountId() != null) {
            account = accountRepository.findById(request.getAccountId())
                    .orElseThrow(() -> new ResourceNotFoundException("Account", request.getAccountId()));
            if (!account.getUser().getId().equals(userId)) {
                throw new UnauthorizedException("Account does not belong to this user");
            }
        }

        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());
        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setType(request.getType());
        transaction.setCategory(category);
        transaction.setAccount(account);
        transaction.setNotes(request.getNotes());
        transaction.setTags(request.getTags());

        Transaction updated = transactionRepository.save(transaction);
        log.info("Transaction updated with id: {} for userId: {}", transactionId, userId);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteTransaction(Long transactionId, Long userId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", transactionId));

        if (!transaction.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Transaction does not belong to this user");
        }

        transactionRepository.delete(transaction);
        log.info("Transaction deleted with id: {} for userId: {}", transactionId, userId);
    }

    public TransactionResponse getTransactionById(Long id, Long userId) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));

        if (!transaction.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Transaction does not belong to this user");
        }

        return mapToResponse(transaction);
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .transactionDate(transaction.getTransactionDate())
                .type(transaction.getType())
                .categoryId(transaction.getCategory() != null ? transaction.getCategory().getId() : null)
                .categoryName(transaction.getCategory() != null ? transaction.getCategory().getName() : null)
                .categoryColor(transaction.getCategory() != null ? transaction.getCategory().getColorHex() : null)
                .accountId(transaction.getAccount() != null ? transaction.getAccount().getId() : null)
                .accountName(transaction.getAccount() != null ? transaction.getAccount().getName() : null)
                .notes(transaction.getNotes())
                .tags(transaction.getTags())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }
}
