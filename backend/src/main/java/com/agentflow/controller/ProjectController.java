package com.agentflow.controller;

import com.agentflow.dto.*;
import com.agentflow.model.User;
import com.agentflow.repository.UserRepository;
import com.agentflow.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final UserRepository userRepository;

    /** Submit a new product idea. Returns the project with credit cost estimate (status: PENDING). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse createProject(@Valid @RequestBody CreateProjectRequest request,
                                         @AuthenticationPrincipal UserDetails principal) {
        return projectService.createProject(request, resolveUser(principal));
    }

    /**
     * Confirm and kick off the agent workflow. Credits are deducted here — not at project creation.
     * Runs async — poll GET /{id} to watch status progress.
     */
    @PostMapping("/{id}/start")
    public ProjectResponse startWorkflow(@PathVariable Long id,
                                         @AuthenticationPrincipal UserDetails principal) {
        return projectService.startWorkflow(id, resolveUser(principal));
    }

    @GetMapping
    public List<ProjectResponse> listProjects(@AuthenticationPrincipal UserDetails principal) {
        return projectService.listProjects(resolveUser(principal));
    }

    /** Lightweight status poll — no messages or tasks. */
    @GetMapping("/{id}")
    public ProjectResponse getProject(@PathVariable Long id,
                                      @AuthenticationPrincipal UserDetails principal) {
        return projectService.getProject(id, resolveUser(principal));
    }

    /** Full project detail including all agent messages and task board. */
    @GetMapping("/{id}/detail")
    public ProjectDetailResponse getProjectDetail(@PathVariable Long id,
                                                  @AuthenticationPrincipal UserDetails principal) {
        return projectService.getProjectDetail(id, resolveUser(principal));
    }

    private User resolveUser(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername())
            .orElseThrow(() -> new IllegalStateException("User not found: " + principal.getUsername()));
    }
}
