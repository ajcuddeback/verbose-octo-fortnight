package com.agentflow.repository;

import com.agentflow.model.Project;
import com.agentflow.model.enums.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findAllByOrderByCreatedAtDesc();
    List<Project> findByStatus(ProjectStatus status);
}
