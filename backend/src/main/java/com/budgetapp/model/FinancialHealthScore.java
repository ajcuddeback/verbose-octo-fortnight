package com.budgetapp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "financial_health_scores")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialHealthScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private double overallScore;

    // Max 25 points
    private double savingsRateScore;

    // Max 25 points
    private double budgetAdherenceScore;

    // Max 20 points
    private double goalProgressScore;

    // Max 20 points
    private double emergencyFundScore;

    // Max 10 points
    private double consistencyScore;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime calculatedAt;
}
