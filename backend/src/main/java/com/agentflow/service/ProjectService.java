package com.agentflow.service;

import com.agentflow.config.CreditProperties;
import com.agentflow.dto.*;
import com.agentflow.model.AgentMessage;
import com.agentflow.model.Project;
import com.agentflow.model.Task;
import com.agentflow.model.User;
import com.agentflow.model.enums.ProjectStatus;
import com.agentflow.repository.AgentMessageRepository;
import com.agentflow.repository.ProjectRepository;
import com.agentflow.repository.TaskRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final AgentMessageRepository messageRepository;
    private final TaskRepository taskRepository;
    private final OrchestratorService orchestratorService;
    private final CreditService creditService;
    private final CreditProperties creditProperties;

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request, User user) {
        // Estimate cost upfront and store it on the project so the user can review before starting
        CreditEstimateResponse estimate = creditService.estimate(request.getProductIdea());

        Project project = projectRepository.save(Project.builder()
            .user(user)
            .name(request.getName())
            .productIdea(request.getProductIdea())
            .status(ProjectStatus.PENDING)
            .complexityTier(estimate.getTier())
            .creditCost(estimate.getCreditCost())
            .currentPhaseNote("Project created. Review the credit cost and call /start when ready.")
            .build());

        return toResponse(project);
    }

    @Transactional
    public ProjectResponse startWorkflow(Long projectId, User user) {
        Project project = findAndCheckOwner(projectId, user);

        if (project.getStatus() != ProjectStatus.PENDING) {
            throw new IllegalStateException("Workflow already started for project " + projectId);
        }

        // Hourly workflow rate limit — prevents bulk abuse even within credit budget
        long recentStarts = projectRepository.countByUserAndCreatedAtAfter(
            user, LocalDateTime.now().minusHours(1));
        if (recentStarts >= creditProperties.getMaxWorkflowsPerHour()) {
            throw new IllegalStateException(
                "Hourly workflow limit reached (" + creditProperties.getMaxWorkflowsPerHour()
                    + " per hour). Please wait before starting another.");
        }

        int cost = project.getCreditCost() != null ? project.getCreditCost() : 0;
        creditService.deductCredits(user, cost, project); // throws if insufficient

        orchestratorService.runWorkflow(project);
        return toResponse(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listProjects(User user) {
        return projectRepository.findByUserOrderByCreatedAtDesc(user)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(Long projectId, User user) {
        return toResponse(findAndCheckOwner(projectId, user));
    }

    @Transactional(readOnly = true)
    public ProjectDetailResponse getProjectDetail(Long projectId, User user) {
        Project project = findAndCheckOwner(projectId, user);
        List<AgentMessage> messages = messageRepository.findByProjectIdOrderBySentAtAsc(projectId);
        List<Task> tasks = taskRepository.findByProjectIdOrderByCreatedAtAsc(projectId);

        return ProjectDetailResponse.builder()
            .id(project.getId())
            .name(project.getName())
            .productIdea(project.getProductIdea())
            .status(project.getStatus())
            .complexityTier(project.getComplexityTier())
            .creditCost(project.getCreditCost())
            .currentPhaseNote(project.getCurrentPhaseNote())
            .messages(messages.stream().map(this::toMessageResponse).toList())
            .tasks(tasks.stream().map(this::toTaskResponse).toList())
            .createdAt(project.getCreatedAt())
            .updatedAt(project.getUpdatedAt())
            .build();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Project findAndCheckOwner(Long projectId, User user) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new EntityNotFoundException("Project not found: " + projectId));
        if (!project.getUser().getId().equals(user.getId())) {
            throw new EntityNotFoundException("Project not found: " + projectId); // intentionally vague
        }
        return project;
    }

    private ProjectResponse toResponse(Project p) {
        return ProjectResponse.builder()
            .id(p.getId())
            .name(p.getName())
            .productIdea(p.getProductIdea())
            .status(p.getStatus())
            .complexityTier(p.getComplexityTier())
            .creditCost(p.getCreditCost())
            .currentPhaseNote(p.getCurrentPhaseNote())
            .createdAt(p.getCreatedAt())
            .updatedAt(p.getUpdatedAt())
            .build();
    }

    private MessageResponse toMessageResponse(AgentMessage m) {
        return MessageResponse.builder()
            .id(m.getId())
            .fromAgentName(m.getFromAgent().getName())
            .fromAgentRole(m.getFromAgent().getRole().name())
            .toAgentName(m.getToAgent() != null ? m.getToAgent().getName() : null)
            .toAgentRole(m.getToAgent() != null ? m.getToAgent().getRole().name() : null)
            .content(m.getContent())
            .messageType(m.getMessageType())
            .sentAt(m.getSentAt())
            .build();
    }

    private TaskResponse toTaskResponse(Task t) {
        return TaskResponse.builder()
            .id(t.getId())
            .title(t.getTitle())
            .description(t.getDescription())
            .status(t.getStatus())
            .assignedAgentName(t.getAssignedAgent() != null ? t.getAssignedAgent().getName() : null)
            .assignedAgentRole(t.getAssignedAgent() != null ? t.getAssignedAgent().getRole().name() : null)
            .createdAt(t.getCreatedAt())
            .updatedAt(t.getUpdatedAt())
            .build();
    }
}
