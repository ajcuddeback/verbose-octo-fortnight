package com.agentflow.dto;

import com.agentflow.model.enums.MessageType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MessageResponse {
    private Long id;
    private String fromAgentName;
    private String fromAgentRole;
    private String toAgentName;   // null = broadcast
    private String toAgentRole;
    private String content;
    private MessageType messageType;
    private LocalDateTime sentAt;
}
