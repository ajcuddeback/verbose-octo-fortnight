package com.agentflow.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "credit_balances")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    /** Current spendable credits. */
    private int balance;

    /** Credits granted per monthly reset cycle (driven by subscription plan). */
    private int monthlyAllowance;

    /** Date the balance was last topped up; used to detect when a new month has started. */
    private LocalDate lastResetDate;

    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
