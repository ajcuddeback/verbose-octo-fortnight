package com.agentflow.config;

import com.agentflow.model.Agent;
import com.agentflow.model.CreditBalance;
import com.agentflow.model.Subscription;
import com.agentflow.model.User;
import com.agentflow.model.enums.*;
import com.agentflow.repository.AgentRepository;
import com.agentflow.repository.CreditBalanceRepository;
import com.agentflow.repository.SubscriptionRepository;
import com.agentflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final AgentRepository agentRepository;
    private final UserRepository userRepository;
    private final CreditBalanceRepository creditBalanceRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PasswordEncoder passwordEncoder;
    private final CreditProperties creditProperties;

    @Override
    public void run(String... args) {
        seedAgents();
        seedDemoUser();
    }

    private void seedAgents() {
        if (agentRepository.count() > 0) return;

        List<Agent> agents = List.of(
            Agent.builder().name("Aria").role(AgentRole.ORCHESTRATOR)
                .description("Central coordinator. Routes tasks, tracks workflow state, ensures nothing falls through the cracks.")
                .specialty("Workflow Orchestration").build(),

            Agent.builder().name("Max").role(AgentRole.PRODUCT_MANAGER)
                .description("Turns product ideas into clear, actionable requirements. Owns product vision and acceptance criteria.")
                .specialty("Requirements & Product Strategy").build(),

            Agent.builder().name("Jordan").role(AgentRole.PROJECT_MANAGER)
                .description("Breaks requirements into tasks, assigns them to the right agents, tracks delivery.")
                .specialty("Sprint Planning & Delivery").build(),

            Agent.builder().name("Luna").role(AgentRole.DESIGNER)
                .description("Defines UI/UX: information architecture, component design, and accessibility standards.")
                .specialty("UI/UX & Design Systems").build(),

            Agent.builder().name("Kai").role(AgentRole.SOFTWARE_ENGINEER)
                .description("Full-stack engineer specializing in backend APIs, database design, and system architecture.")
                .specialty("Backend / Full-Stack").build(),

            Agent.builder().name("River").role(AgentRole.SOFTWARE_ENGINEER)
                .description("Frontend engineer specializing in modern frameworks, component libraries, and performance.")
                .specialty("Frontend").build(),

            Agent.builder().name("Quinn").role(AgentRole.QA_ENGINEER)
                .description("Designs and executes test strategies: unit, integration, E2E, and security testing.")
                .specialty("Testing & Quality Assurance").build(),

            Agent.builder().name("Sam").role(AgentRole.DEVOPS)
                .description("Owns CI/CD pipelines, cloud infrastructure, security hardening, and production deployments.")
                .specialty("Cloud Infrastructure & Security").build()
        );

        agentRepository.saveAll(agents);
        log.info("Agent roster seeded: {} agents", agents.size());
    }

    private void seedDemoUser() {
        if (userRepository.existsByEmail("demo@agentflow.dev")) return;

        User demo = userRepository.save(User.builder()
            .email("demo@agentflow.dev")
            .passwordHash(passwordEncoder.encode("demo1234"))
            .firstName("Demo")
            .lastName("User")
            .role(UserRole.USER)
            .build());

        creditBalanceRepository.save(CreditBalance.builder()
            .user(demo)
            .balance(creditProperties.getFreeMonthlyAllowance())
            .monthlyAllowance(creditProperties.getFreeMonthlyAllowance())
            .lastResetDate(LocalDate.now())
            .build());

        subscriptionRepository.save(Subscription.builder()
            .user(demo)
            .plan(SubscriptionPlan.FREE)
            .status(SubscriptionStatus.ACTIVE)
            .currentPeriodStart(LocalDateTime.now())
            .currentPeriodEnd(LocalDateTime.now().plusMonths(1))
            .build());

        log.info("Demo user seeded — email: demo@agentflow.dev  password: demo1234");
    }
}
