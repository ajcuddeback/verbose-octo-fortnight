package com.agentflow.repository;

import com.agentflow.model.CreditBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CreditBalanceRepository extends JpaRepository<CreditBalance, Long> {
    Optional<CreditBalance> findByUserId(Long userId);
}
