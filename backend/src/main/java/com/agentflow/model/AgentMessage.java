package com.agentflow.model;

import com.agentflow.model.enums.MessageType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "from_agent_id", nullable = false)
    private Agent fromAgent;

    // null = broadcast to all agents
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "to_agent_id")
    private Agent toAgent;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType messageType;

    private LocalDateTime sentAt;

    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
    }
}
