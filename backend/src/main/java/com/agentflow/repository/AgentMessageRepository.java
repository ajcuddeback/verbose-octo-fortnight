package com.agentflow.repository;

import com.agentflow.model.AgentMessage;
import com.agentflow.model.enums.MessageType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentMessageRepository extends JpaRepository<AgentMessage, Long> {
    List<AgentMessage> findByProjectIdOrderBySentAtAsc(Long projectId);
    List<AgentMessage> findByProjectIdAndMessageType(Long projectId, MessageType type);
}
