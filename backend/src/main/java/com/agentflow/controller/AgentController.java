package com.agentflow.controller;

import com.agentflow.dto.AgentResponse;
import com.agentflow.model.Agent;
import com.agentflow.repository.AgentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentRepository agentRepository;

    @GetMapping
    public List<AgentResponse> listAgents() {
        return agentRepository.findAllByOrderByRoleAsc()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @GetMapping("/{id}")
    public AgentResponse getAgent(@PathVariable Long id) {
        Agent agent = agentRepository.findById(id)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Agent not found: " + id));
        return toResponse(agent);
    }

    private AgentResponse toResponse(Agent a) {
        return AgentResponse.builder()
            .id(a.getId())
            .name(a.getName())
            .role(a.getRole())
            .description(a.getDescription())
            .specialty(a.getSpecialty())
            .build();
    }
}
