package com.agentflow.config;

import com.agentflow.model.Agent;
import com.agentflow.model.enums.AgentRole;
import com.agentflow.repository.AgentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds the agent roster on startup.
 * Add or modify agents here as the team grows.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final AgentRepository agentRepository;

    @Override
    public void run(String... args) {
        if (agentRepository.count() > 0) return;

        List<Agent> agents = List.of(
            Agent.builder()
                .name("Aria")
                .role(AgentRole.ORCHESTRATOR)
                .description("Central coordinator. Routes tasks between agents, tracks workflow state, and ensures nothing falls through the cracks.")
                .specialty("Workflow Orchestration")
                .build(),

            Agent.builder()
                .name("Max")
                .role(AgentRole.PRODUCT_MANAGER)
                .description("Turns product ideas into clear, actionable requirements. Owns the product vision and acceptance criteria.")
                .specialty("Requirements & Product Strategy")
                .build(),

            Agent.builder()
                .name("Jordan")
                .role(AgentRole.PROJECT_MANAGER)
                .description("Breaks requirements into tasks, assigns them to the right agents, and tracks delivery against the plan.")
                .specialty("Sprint Planning & Delivery")
                .build(),

            Agent.builder()
                .name("Luna")
                .role(AgentRole.DESIGNER)
                .description("Defines UI/UX: information architecture, component design, and accessibility standards.")
                .specialty("UI/UX & Design Systems")
                .build(),

            Agent.builder()
                .name("Kai")
                .role(AgentRole.SOFTWARE_ENGINEER)
                .description("Full-stack engineer specializing in backend APIs, database design, and system architecture.")
                .specialty("Backend / Full-Stack")
                .build(),

            Agent.builder()
                .name("River")
                .role(AgentRole.SOFTWARE_ENGINEER)
                .description("Frontend engineer specializing in modern frameworks, component libraries, and performance optimization.")
                .specialty("Frontend")
                .build(),

            Agent.builder()
                .name("Quinn")
                .role(AgentRole.QA_ENGINEER)
                .description("Designs and executes test strategies covering unit, integration, E2E, and security testing.")
                .specialty("Testing & Quality Assurance")
                .build(),

            Agent.builder()
                .name("Sam")
                .role(AgentRole.DEVOPS)
                .description("Owns CI/CD pipelines, cloud infrastructure, security hardening, and production deployments.")
                .specialty("Cloud Infrastructure & Security")
                .build()
        );

        agentRepository.saveAll(agents);
        log.info("Agent roster seeded: {} agents", agents.size());
    }
}
