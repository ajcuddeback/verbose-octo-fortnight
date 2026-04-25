package com.agentflow.controller;

import com.agentflow.dto.*;
import com.agentflow.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    /** Submit a new product idea. Returns the created project (status: PENDING). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse createProject(@Valid @RequestBody CreateProjectRequest request) {
        return projectService.createProject(request);
    }

    /** Kick off the agent workflow for a pending project. Runs async — poll GET for updates. */
    @PostMapping("/{id}/start")
    public ProjectResponse startWorkflow(@PathVariable Long id) {
        return projectService.startWorkflow(id);
    }

    /** List all projects (summary only). */
    @GetMapping
    public List<ProjectResponse> listProjects() {
        return projectService.listProjects();
    }

    /** Get project status (lightweight — no messages or tasks). */
    @GetMapping("/{id}")
    public ProjectResponse getProject(@PathVariable Long id) {
        return projectService.getProject(id);
    }

    /** Get full project detail including all agent messages and tasks. */
    @GetMapping("/{id}/detail")
    public ProjectDetailResponse getProjectDetail(@PathVariable Long id) {
        return projectService.getProjectDetail(id);
    }
}
