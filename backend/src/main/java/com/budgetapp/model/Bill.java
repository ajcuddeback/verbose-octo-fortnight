package com.budgetapp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bills")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String name;

    private BigDecimal amount;

    // Day of month (1-31)
    private int dueDay;

    @Enumerated(EnumType.STRING)
    private BillFrequency frequency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    private boolean isAutoPay;

    @Builder.Default
    private boolean isActive = true;

    private LocalDate lastPaidDate;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
