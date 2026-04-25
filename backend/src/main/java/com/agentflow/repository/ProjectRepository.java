package com.agentflow.repository;

import com.agentflow.model.Project;
import com.agentflow.model.User;
import com.agentflow.model.enums.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByUserOrderByCreatedAtDesc(User user);
    List<Project> findAllByOrderByCreatedAtDesc();
    List<Project> findByStatus(ProjectStatus status);

    /** Used to enforce the per-user hourly workflow-start rate limit. */
    long countByUserAndCreatedAtAfter(User user, LocalDateTime after);
}
