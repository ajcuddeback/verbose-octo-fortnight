package com.agentflow.dto;

import com.agentflow.model.enums.ProjectStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProjectResponse {
    private Long id;
    private String name;
    private String productIdea;
    private ProjectStatus status;
    private String currentPhaseNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
