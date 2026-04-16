package com.budgetapp.service;

import com.budgetapp.dto.request.TransactionRequest;
import com.budgetapp.dto.response.TransactionResponse;
import com.budgetapp.exception.UnauthorizedException;
import com.budgetapp.model.*;
import com.budgetapp.repository.AccountRepository;
import com.budgetapp.repository.CategoryRepository;
import com.budgetapp.repository.TransactionRepository;
import com.budgetapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User testUser;
    private User otherUser;
    private Transaction testTransaction;
    private TransactionRequest transactionRequest;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .subscriptionStatus(SubscriptionStatus.ACTIVE)
                .build();

        otherUser = User.builder()
                .id(2L)
                .email("other@example.com")
                .subscriptionStatus(SubscriptionStatus.ACTIVE)
                .build();

        testCategory = Category.builder()
                .id(1L)
                .name("Food")
                .type(TransactionType.EXPENSE)
                .colorHex("#FF5722")
                .build();

        testTransaction = Transaction.builder()
                .id(1L)
                .user(testUser)
                .amount(new BigDecimal("50.00"))
                .description("Grocery shopping")
                .transactionDate(LocalDate.now())
                .type(TransactionType.EXPENSE)
                .category(testCategory)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        transactionRequest = new TransactionRequest();
        transactionRequest.setAmount(new BigDecimal("50.00"));
        transactionRequest.setDescription("Grocery shopping");
        transactionRequest.setTransactionDate(LocalDate.now());
        transactionRequest.setType(TransactionType.EXPENSE);
        transactionRequest.setCategoryId(1L);
    }

    @Test
    void createTransaction_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // Act
        TransactionResponse response = transactionService.createTransaction(transactionRequest, 1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(response.getDescription()).isEqualTo("Grocery shopping");
        assertThat(response.getType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(response.getCategoryName()).isEqualTo("Food");

        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void createTransaction_WithoutCategory_Success() {
        // Arrange
        transactionRequest.setCategoryId(null);
        Transaction transactionWithoutCategory = Transaction.builder()
                .id(2L)
                .user(testUser)
                .amount(new BigDecimal("50.00"))
                .description("Grocery shopping")
                .transactionDate(LocalDate.now())
                .type(TransactionType.EXPENSE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transactionWithoutCategory);

        // Act
        TransactionResponse response = transactionService.createTransaction(transactionRequest, 1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getCategoryName()).isNull();
        verify(categoryRepository, never()).findById(any());
    }

    @Test
    void getUserTransactions_ReturnsAllUserTransactions() {
        // Arrange
        Transaction tx2 = Transaction.builder()
                .id(2L)
                .user(testUser)
                .amount(new BigDecimal("100.00"))
                .description("Salary")
                .transactionDate(LocalDate.now())
                .type(TransactionType.INCOME)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(transactionRepository.findByUserOrderByTransactionDateDesc(testUser))
                .thenReturn(List.of(testTransaction, tx2));

        // Act
        List<TransactionResponse> responses = transactionService.getUserTransactions(1L, null, null, null);

        // Assert
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(responses.get(1).getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void getUserTransactions_WithDateFilter() {
        // Arrange
        LocalDate from = LocalDate.now().minusDays(7);
        LocalDate to = LocalDate.now();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(transactionRepository.findByUserAndTransactionDateBetween(testUser, from, to))
                .thenReturn(List.of(testTransaction));

        // Act
        List<TransactionResponse> responses = transactionService.getUserTransactions(1L, from, to, null);

        // Assert
        assertThat(responses).hasSize(1);
        verify(transactionRepository).findByUserAndTransactionDateBetween(testUser, from, to);
    }

    @Test
    void updateTransaction_OwnershipCheck_Success() {
        // Arrange
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // Act
        TransactionResponse response = transactionService.updateTransaction(1L, transactionRequest, 1L);

        // Assert
        assertThat(response).isNotNull();
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void updateTransaction_WrongOwner_ThrowsUnauthorizedException() {
        // Arrange
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));

        // Act & Assert - testTransaction belongs to userId 1, but we're using userId 2
        assertThatThrownBy(() -> transactionService.updateTransaction(1L, transactionRequest, 2L))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Transaction does not belong to this user");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void deleteTransaction_Success() {
        // Arrange
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));

        // Act
        transactionService.deleteTransaction(1L, 1L);

        // Assert
        verify(transactionRepository).delete(testTransaction);
    }

    @Test
    void deleteTransaction_WrongOwner_ThrowsUnauthorizedException() {
        // Arrange
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));

        // Act & Assert
        assertThatThrownBy(() -> transactionService.deleteTransaction(1L, 2L))
                .isInstanceOf(UnauthorizedException.class);

        verify(transactionRepository, never()).delete(any());
    }
}
