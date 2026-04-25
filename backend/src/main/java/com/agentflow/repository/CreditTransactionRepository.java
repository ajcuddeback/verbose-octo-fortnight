package com.agentflow.repository;

import com.agentflow.model.CreditTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, Long> {
    List<CreditTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);
}
