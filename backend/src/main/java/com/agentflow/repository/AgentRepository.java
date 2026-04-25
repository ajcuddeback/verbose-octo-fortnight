package com.agentflow.repository;

import com.agentflow.model.Agent;
import com.agentflow.model.enums.AgentRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgentRepository extends JpaRepository<Agent, Long> {
    List<Agent> findByRole(AgentRole role);
    Optional<Agent> findFirstByRole(AgentRole role);
    List<Agent> findAllByOrderByRoleAsc();
}
