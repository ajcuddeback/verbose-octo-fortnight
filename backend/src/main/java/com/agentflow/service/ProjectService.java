package com.agentflow.service;

import com.agentflow.dto.*;
import com.agentflow.model.AgentMessage;
import com.agentflow.model.Project;
import com.agentflow.model.Task;
import com.agentflow.model.enums.ProjectStatus;
import com.agentflow.repository.AgentMessageRepository;
import com.agentflow.repository.ProjectRepository;
import com.agentflow.repository.TaskRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final AgentMessageRepository messageRepository;
    private final TaskRepository taskRepository;
    private final OrchestratorService orchestratorService;

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request) {
        Project project = projectRepository.save(Project.builder()
            .name(request.getName())
            .productIdea(request.getProductIdea())
            .status(ProjectStatus.PENDING)
            .currentPhaseNote("Project created. Kick off the workflow when ready.")
            .build());

        return toResponse(project);
    }

    @Transactional
    public ProjectResponse startWorkflow(Long projectId) {
        Project project = findOrThrow(projectId);
        if (project.getStatus() != ProjectStatus.PENDING) {
            throw new IllegalStateException("Workflow already started for project " + projectId);
        }
        orchestratorService.runWorkflow(project);
        return toResponse(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listProjects() {
        return projectRepository.findAllByOrderByCreatedAtDesc()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(Long projectId) {
        return toResponse(findOrThrow(projectId));
    }

    @Transactional(readOnly = true)
    public ProjectDetailResponse getProjectDetail(Long projectId) {
        Project project = findOrThrow(projectId);
        List<AgentMessage> messages = messageRepository.findByProjectIdOrderBySentAtAsc(projectId);
        List<Task> tasks = taskRepository.findByProjectIdOrderByCreatedAtAsc(projectId);
        return ProjectDetailResponse.builder()
            .id(project.getId())
            .name(project.getName())
            .productIdea(project.getProductIdea())
            .status(project.getStatus())
            .currentPhaseNote(project.getCurrentPhaseNote())
            .messages(messages.stream().map(this::toMessageResponse).toList())
            .tasks(tasks.stream().map(this::toTaskResponse).toList())
            .createdAt(project.getCreatedAt())
            .updatedAt(project.getUpdatedAt())
            .build();
    }

    // -------------------------------------------------------------------------
    // Mappers
    // -------------------------------------------------------------------------
    private ProjectResponse toResponse(Project p) {
        return ProjectResponse.builder()
            .id(p.getId())
            .name(p.getName())
            .productIdea(p.getProductIdea())
            .status(p.getStatus())
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

    private Project findOrThrow(Long id) {
        return projectRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Project not found: " + id));
    }
}
