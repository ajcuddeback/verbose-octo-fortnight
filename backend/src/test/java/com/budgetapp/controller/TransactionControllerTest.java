package com.budgetapp.controller;

import com.budgetapp.config.JwtProperties;
import com.budgetapp.dto.request.TransactionRequest;
import com.budgetapp.dto.response.TransactionResponse;
import com.budgetapp.model.SubscriptionStatus;
import com.budgetapp.model.TransactionType;
import com.budgetapp.model.User;
import com.budgetapp.repository.UserRepository;
import com.budgetapp.security.JwtAuthenticationFilter;
import com.budgetapp.security.JwtTokenProvider;
import com.budgetapp.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = TransactionController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtProperties jwtProperties;

    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    private ObjectMapper objectMapper;
    private User testUser;
    private TransactionResponse testTransactionResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .subscriptionStatus(SubscriptionStatus.ACTIVE)
                .build();

        testTransactionResponse = TransactionResponse.builder()
                .id(1L)
                .amount(new BigDecimal("75.50"))
                .description("Grocery shopping")
                .transactionDate(LocalDate.now())
                .type(TransactionType.EXPENSE)
                .categoryId(1L)
                .categoryName("Food")
                .categoryColor("#FF5722")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void getTransactions_Authenticated_Returns200() throws Exception {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(transactionService.getUserTransactions(anyLong(), any(), any(), any()))
                .thenReturn(List.of(testTransactionResponse));

        // Act & Assert
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].amount").value(75.50))
                .andExpect(jsonPath("$[0].description").value("Grocery shopping"))
                .andExpect(jsonPath("$[0].categoryName").value("Food"));
    }

    @Test
    void getTransactions_Unauthenticated_Returns401() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void createTransaction_ValidRequest_Returns201() throws Exception {
        // Arrange
        TransactionRequest request = new TransactionRequest();
        request.setAmount(new BigDecimal("75.50"));
        request.setDescription("Grocery shopping");
        request.setTransactionDate(LocalDate.now());
        request.setType(TransactionType.EXPENSE);
        request.setCategoryId(1L);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(transactionService.createTransaction(any(TransactionRequest.class), anyLong()))
                .thenReturn(testTransactionResponse);

        // Act & Assert
        mockMvc.perform(post("/api/transactions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount").value(75.50))
                .andExpect(jsonPath("$.description").value("Grocery shopping"))
                .andExpect(jsonPath("$.type").value("EXPENSE"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void createTransaction_MissingAmount_Returns400() throws Exception {
        // Arrange - amount is null
        TransactionRequest request = new TransactionRequest();
        request.setDescription("Grocery shopping");
        request.setTransactionDate(LocalDate.now());
        request.setType(TransactionType.EXPENSE);
        // amount not set

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act & Assert
        mockMvc.perform(post("/api/transactions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void getTransactionById_ExistingTransaction_Returns200() throws Exception {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(transactionService.getTransactionById(1L, 1L)).thenReturn(testTransactionResponse);

        // Act & Assert
        mockMvc.perform(get("/api/transactions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.description").value("Grocery shopping"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void deleteTransaction_ExistingTransaction_Returns204() throws Exception {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act & Assert
        mockMvc.perform(delete("/api/transactions/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void getTransactions_WithFilters_Returns200() throws Exception {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(transactionService.getUserTransactions(anyLong(), any(), any(), any()))
                .thenReturn(List.of(testTransactionResponse));

        // Act & Assert
        mockMvc.perform(get("/api/transactions")
                        .param("from", LocalDate.now().minusDays(30).toString())
                        .param("to", LocalDate.now().toString())
                        .param("type", "EXPENSE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
