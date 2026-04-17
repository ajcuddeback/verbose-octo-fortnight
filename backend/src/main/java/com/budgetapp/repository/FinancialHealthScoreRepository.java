package com.budgetapp.repository;

import com.budgetapp.model.FinancialHealthScore;
import com.budgetapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialHealthScoreRepository extends JpaRepository<FinancialHealthScore, Long> {

    Optional<FinancialHealthScore> findTopByUserOrderByCalculatedAtDesc(User user);

    List<FinancialHealthScore> findByUserAndCalculatedAtBetween(
            User user, LocalDateTime startDate, LocalDateTime endDate);

    List<FinancialHealthScore> findByUser(User user);
}
