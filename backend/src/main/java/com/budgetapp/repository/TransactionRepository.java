package com.budgetapp.repository;

import com.budgetapp.model.Transaction;
import com.budgetapp.model.TransactionType;
import com.budgetapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserAndTransactionDateBetween(
            User user, LocalDate startDate, LocalDate endDate);

    List<Transaction> findByUserOrderByTransactionDateDesc(User user);

    List<Transaction> findByUserAndType(User user, TransactionType type);

    @Query(value = "SELECT COALESCE(SUM(t.amount), 0) FROM transactions t " +
            "WHERE t.user_id = :userId AND t.type = :type " +
            "AND t.transaction_date >= :startDate AND t.transaction_date <= :endDate",
            nativeQuery = true)
    BigDecimal sumAmountByUserAndTypeAndDateBetween(
            @Param("userId") Long userId,
            @Param("type") String type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    List<Transaction> findByUserAndTypeAndTransactionDateBetween(
            User user, TransactionType type, LocalDate startDate, LocalDate endDate);

    @Query("SELECT t FROM Transaction t WHERE t.user = :user " +
            "AND t.category.id = :categoryId " +
            "AND t.transactionDate >= :startDate AND t.transactionDate <= :endDate")
    List<Transaction> findByUserAndCategoryIdAndDateBetween(
            @Param("user") User user,
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
