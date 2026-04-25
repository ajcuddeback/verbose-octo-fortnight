package com.agentflow.service;

import com.agentflow.model.Agent;
import com.agentflow.model.AgentMessage;
import com.agentflow.model.Project;
import com.agentflow.model.Task;
import com.agentflow.model.enums.*;
import com.agentflow.repository.AgentMessageRepository;
import com.agentflow.repository.AgentRepository;
import com.agentflow.repository.ProjectRepository;
import com.agentflow.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Central orchestrator — drives the agent workflow for a project.
 * Each phase is handed off to the appropriate agent role in sequence.
 *
 * TODO: Replace stub content generation with real LLM calls (e.g. Claude API).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrchestratorService {

    private final ProjectRepository projectRepository;
    private final AgentRepository agentRepository;
    private final AgentMessageRepository messageRepository;
    private final TaskRepository taskRepository;

    @Async
    public void runWorkflow(Project project) {
        log.info("Starting workflow for project: {} (id={})", project.getName(), project.getId());
        try {
            productManagerPhase(project);
            projectManagerPhase(project);
            designerPhase(project);
            developmentPhase(project);
            qaPhase(project);
            devOpsPhase(project);
            log.info("Workflow complete for project: {}", project.getName());
        } catch (Exception e) {
            log.error("Workflow failed for project {}: {}", project.getName(), e.getMessage(), e);
            setStatus(project, ProjectStatus.FAILED, "Workflow encountered an unexpected error: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Phase 1 — Product Manager: gather & define requirements
    // -------------------------------------------------------------------------
    private void productManagerPhase(Project project) throws InterruptedException {
        setStatus(project, ProjectStatus.GATHERING_REQUIREMENTS, "Product Manager is analyzing the idea and defining requirements.");
        Agent orchestrator = agent(AgentRole.ORCHESTRATOR);
        Agent pm = agent(AgentRole.PRODUCT_MANAGER);

        post(project, orchestrator, pm,
            "Hi! We have a new project: \"" + project.getName() + "\". Please analyze this idea and define requirements.\n\nIdea: " + project.getProductIdea(),
            MessageType.STATUS_UPDATE);

        sleep(1500);

        post(project, pm, null, buildRequirements(project), MessageType.REQUIREMENT);
        sleep(500);
    }

    // -------------------------------------------------------------------------
    // Phase 2 — Project Manager: plan and create tasks
    // -------------------------------------------------------------------------
    private void projectManagerPhase(Project project) throws InterruptedException {
        setStatus(project, ProjectStatus.PLANNING, "Project Manager is breaking down work into tasks.");
        Agent orchestrator = agent(AgentRole.ORCHESTRATOR);
        Agent pm = agent(AgentRole.PROJECT_MANAGER);

        post(project, orchestrator, pm,
            "Requirements are ready. Please create a task breakdown for \"" + project.getName() + "\".",
            MessageType.STATUS_UPDATE);

        sleep(1500);

        List<String[]> taskDefs = List.of(
            new String[]{"Project repository & CI/CD setup", "Initialize repo, configure GitHub Actions pipeline, add branch protection rules."},
            new String[]{"Database schema design", "Design and document the data model. Create migration scripts."},
            new String[]{"Core API implementation", "Implement REST endpoints for all primary resources with validation and error handling."},
            new String[]{"Frontend UI — core screens", "Build primary user-facing views and routing."},
            new String[]{"Frontend UI — secondary screens", "Build settings, profile, and auxiliary views."},
            new String[]{"Unit tests — backend", "Achieve ≥80% line coverage on service and controller layers."},
            new String[]{"Integration tests", "End-to-end flow tests covering critical user journeys."},
            new String[]{"Security hardening", "Input sanitization, rate limiting, dependency audit."},
            new String[]{"Staging deployment", "Deploy to staging environment and smoke-test."},
            new String[]{"Production deployment", "Blue/green production deploy with rollback plan."}
        );

        Agent se1 = agentRepository.findByRole(AgentRole.SOFTWARE_ENGINEER).get(0);
        Agent se2 = agentRepository.findByRole(AgentRole.SOFTWARE_ENGINEER).get(1);
        Agent devops = agent(AgentRole.DEVOPS);

        Agent[] assignees = {devops, se1, se1, se2, se2, se1, se2, devops, devops, devops};

        StringBuilder taskSummary = new StringBuilder("Task breakdown for **" + project.getName() + "**:\n\n");
        for (int i = 0; i < taskDefs.size(); i++) {
            String[] def = taskDefs.get(i);
            Agent assignee = assignees[i];
            Task task = taskRepository.save(Task.builder()
                .project(project)
                .title(def[0])
                .description(def[1])
                .status(TaskStatus.BACKLOG)
                .assignedAgent(assignee)
                .build());
            taskSummary.append(String.format("- [%d] %s → assigned to %s%n", task.getId(), def[0], assignee.getName()));
        }

        post(project, pm, null, taskSummary.toString(), MessageType.TASK_ASSIGNMENT);
        sleep(500);
    }

    // -------------------------------------------------------------------------
    // Phase 3 — Designer: UI/UX specification
    // -------------------------------------------------------------------------
    private void designerPhase(Project project) throws InterruptedException {
        setStatus(project, ProjectStatus.DESIGNING, "Designer is producing UI/UX specifications.");
        Agent orchestrator = agent(AgentRole.ORCHESTRATOR);
        Agent designer = agent(AgentRole.DESIGNER);

        post(project, orchestrator, designer,
            "Tasks are planned. Please produce design specs for \"" + project.getName() + "\".",
            MessageType.STATUS_UPDATE);

        sleep(2000);
        post(project, designer, null, buildDesignSpec(project), MessageType.DESIGN_SPEC);
        sleep(500);
    }

    // -------------------------------------------------------------------------
    // Phase 4 — Software Engineers: implement assigned tasks
    // -------------------------------------------------------------------------
    private void developmentPhase(Project project) throws InterruptedException {
        setStatus(project, ProjectStatus.IN_DEVELOPMENT, "Engineers are implementing the project.");
        List<Agent> engineers = agentRepository.findByRole(AgentRole.SOFTWARE_ENGINEER);

        List<Task> tasks = taskRepository.findByProjectIdOrderByCreatedAtAsc(project.getId());

        for (Task task : tasks) {
            if (task.getAssignedAgent().getRole() != AgentRole.SOFTWARE_ENGINEER) continue;

            task.setStatus(TaskStatus.IN_PROGRESS);
            taskRepository.save(task);

            post(project, task.getAssignedAgent(), null,
                "Starting work on: **" + task.getTitle() + "**. " + task.getDescription(),
                MessageType.STATUS_UPDATE);

            sleep(1200);

            task.setStatus(TaskStatus.IN_REVIEW);
            taskRepository.save(task);
            sleep(600);

            task.setStatus(TaskStatus.DONE);
            taskRepository.save(task);

            post(project, task.getAssignedAgent(), agent(AgentRole.ORCHESTRATOR),
                "Completed: **" + task.getTitle() + "**. Code reviewed and merged.",
                MessageType.CODE_COMPLETE);
            sleep(400);
        }
    }

    // -------------------------------------------------------------------------
    // Phase 5 — QA Engineer: testing
    // -------------------------------------------------------------------------
    private void qaPhase(Project project) throws InterruptedException {
        setStatus(project, ProjectStatus.QA_TESTING, "QA is running tests.");
        Agent orchestrator = agent(AgentRole.ORCHESTRATOR);
        Agent qa = agent(AgentRole.QA_ENGINEER);

        post(project, orchestrator, qa,
            "Development is complete. Please run QA on \"" + project.getName() + "\".",
            MessageType.STATUS_UPDATE);

        sleep(2000);
        post(project, qa, null, buildQAReport(project), MessageType.QA_REPORT);
        sleep(500);
    }

    // -------------------------------------------------------------------------
    // Phase 6 — DevOps: deploy and secure
    // -------------------------------------------------------------------------
    private void devOpsPhase(Project project) throws InterruptedException {
        setStatus(project, ProjectStatus.DEPLOYING, "DevOps is deploying and securing the application.");
        Agent orchestrator = agent(AgentRole.ORCHESTRATOR);
        Agent devops = agent(AgentRole.DEVOPS);

        post(project, orchestrator, devops,
            "QA passed. Please deploy \"" + project.getName() + "\" to production.",
            MessageType.STATUS_UPDATE);

        // Mark DevOps tasks in progress then done
        List<Task> devopsTasks = taskRepository.findByProjectIdOrderByCreatedAtAsc(project.getId())
            .stream()
            .filter(t -> t.getAssignedAgent().getRole() == AgentRole.DEVOPS)
            .toList();

        for (Task task : devopsTasks) {
            task.setStatus(TaskStatus.IN_PROGRESS);
            taskRepository.save(task);
            sleep(800);
            task.setStatus(TaskStatus.DONE);
            taskRepository.save(task);
        }

        sleep(1000);
        post(project, devops, null, buildDeploymentReport(project), MessageType.DEPLOYMENT_REPORT);

        setStatus(project, ProjectStatus.DEPLOYED, "Project is live in production.");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------
    private Agent agent(AgentRole role) {
        return agentRepository.findFirstByRole(role)
            .orElseThrow(() -> new IllegalStateException("No agent found for role: " + role));
    }

    private AgentMessage post(Project project, Agent from, Agent to, String content, MessageType type) {
        return messageRepository.save(AgentMessage.builder()
            .project(project)
            .fromAgent(from)
            .toAgent(to)
            .content(content)
            .messageType(type)
            .build());
    }

    private void setStatus(Project project, ProjectStatus status, String note) {
        project.setStatus(status);
        project.setCurrentPhaseNote(note);
        projectRepository.save(project);
        log.info("Project {} → {}", project.getName(), status);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Workflow interrupted", e);
        }
    }

    // -------------------------------------------------------------------------
    // Stub content generators — TODO: replace with LLM calls
    // -------------------------------------------------------------------------
    private String buildRequirements(Project project) {
        return """
            ## Requirements Analysis: %s

            ### Problem Statement
            %s

            ### Functional Requirements
            1. User authentication and account management
            2. Core domain feature set derived from the product idea
            3. RESTful API with versioned endpoints
            4. Persistent data storage with proper indexing
            5. Responsive frontend accessible on desktop and mobile

            ### Non-Functional Requirements
            - **Performance:** p95 API latency < 200ms for read operations
            - **Scalability:** Stateless services for horizontal scaling
            - **Security:** OWASP Top 10 compliance, input validation, rate limiting
            - **Availability:** 99.9%% uptime SLA in production
            - **Observability:** Structured logging, metrics, health endpoints

            ### Acceptance Criteria
            - All core user flows work end-to-end in a clean environment
            - Unit test coverage ≥ 80%% on business logic
            - Zero critical or high security vulnerabilities (OWASP ZAP scan)
            - Lighthouse performance score ≥ 90 on primary pages
            - Accessible at WCAG 2.1 AA standard
            """.formatted(project.getName(), project.getProductIdea());
    }

    private String buildDesignSpec(Project project) {
        return """
            ## UI/UX Design Specification: %s

            ### Design System
            - **Typography:** Inter (headings) / System UI (body)
            - **Color palette:** Primary #2563EB, Surface #F8FAFC, Text #0F172A
            - **Spacing:** 4px base grid
            - **Border radius:** 8px cards, 6px inputs, 24px buttons

            ### Information Architecture
            1. **Onboarding flow** — sign-up → guided setup wizard → dashboard
            2. **Primary dashboard** — summary KPIs, recent activity, quick actions
            3. **Core feature views** — CRUD interfaces per domain entity
            4. **Settings** — profile, notifications, integrations, billing

            ### Component Library
            - Navigation: persistent left sidebar (desktop) / bottom nav (mobile)
            - Data tables with sort, filter, and pagination
            - Form components with inline validation
            - Toast notifications for async feedback
            - Empty states and skeleton loaders

            ### Accessibility
            - All interactive elements keyboard navigable
            - ARIA labels on icon-only buttons
            - Color contrast ratio ≥ 4.5:1 throughout
            - Reduced-motion respect via `prefers-reduced-motion`
            """.formatted(project.getName());
    }

    private String buildQAReport(Project project) {
        return """
            ## QA Test Report: %s

            ### Test Execution Summary
            | Suite              | Tests | Passed | Failed | Skipped |
            |--------------------|-------|--------|--------|---------|
            | Unit — Backend     |   142 |    142 |      0 |       0 |
            | Unit — Frontend    |    87 |     87 |      0 |       0 |
            | Integration        |    34 |     34 |      0 |       0 |
            | E2E                |    21 |     21 |      0 |       0 |
            | **Total**          |   284 |    284 |      0 |       0 |

            ### Coverage
            - Backend line coverage: **84%%**
            - Frontend branch coverage: **81%%**

            ### Security Scan (OWASP ZAP)
            - Critical: 0
            - High: 0
            - Medium: 1 (missing `X-Content-Type-Options` header — fixed)
            - Low: 2 (informational, accepted)

            ### Performance (Lighthouse — Production Build)
            - Performance: 94
            - Accessibility: 97
            - Best Practices: 100
            - SEO: 92

            ### Sign-off
            All acceptance criteria met. ✅ Ready for production deployment.
            """.formatted(project.getName());
    }

    private String buildDeploymentReport(Project project) {
        return """
            ## Deployment Report: %s

            ### Infrastructure
            - **Cloud:** AWS (us-east-1)
            - **Backend:** ECS Fargate — 2 tasks, auto-scaling 2–8
            - **Database:** RDS PostgreSQL 16 (Multi-AZ)
            - **Frontend:** CloudFront + S3 (global CDN)
            - **Secrets:** AWS Secrets Manager (no plaintext credentials in env)

            ### Deployment Strategy
            - Blue/green deployment with automatic rollback on health-check failure
            - Zero-downtime deploy verified ✅
            - Database migrations ran successfully via Flyway ✅

            ### Security Hardening Applied
            - TLS 1.2+ enforced on all endpoints
            - HSTS header configured (max-age=31536000)
            - Security groups: backend only reachable from ALB
            - WAF rules active: rate limiting, SQL injection, XSS filters
            - Dependency vulnerability scan: 0 critical issues

            ### Monitoring & Alerting
            - CloudWatch dashboards: error rate, latency, DB connections
            - PagerDuty alerts: p99 latency > 1s, error rate > 1%%
            - Log aggregation: CloudWatch Logs with 90-day retention

            ### Production URL
            https://%s.example.com  (TODO: update with real domain)

            Deployment complete. 🚀 All systems nominal.
            """.formatted(project.getName(), project.getName().toLowerCase().replaceAll("\\s+", "-"));
    }
}
