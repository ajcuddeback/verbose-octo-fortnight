package com.agentflow.dto;

import com.agentflow.model.enums.ComplexityTier;
import com.agentflow.model.enums.ProjectStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ProjectDetailResponse {
    private Long id;
    private String name;
    private String productIdea;
    private ProjectStatus status;
    private ComplexityTier complexityTier;
    private Integer creditCost;
    private String currentPhaseNote;
    private List<MessageResponse> messages;
    private List<TaskResponse> tasks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
