package com.agentflow.dto;

import com.agentflow.model.enums.TaskStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private String assignedAgentName;
    private String assignedAgentRole;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
