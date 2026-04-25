package com.agentflow.model;

import com.agentflow.model.enums.AgentRole;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "agents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Agent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgentRole role;

    @Column(columnDefinition = "TEXT")
    private String description;

    // e.g. "Frontend", "Backend", "Full-Stack" for engineers
    private String specialty;
}
