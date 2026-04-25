package com.agentflow.dto;

import com.agentflow.model.enums.AgentRole;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentResponse {
    private Long id;
    private String name;
    private AgentRole role;
    private String description;
    private String specialty;
}
