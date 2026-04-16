package com.budgetapp.dto.response;

import com.budgetapp.model.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {

    private SubscriptionStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime cancelledAt;
    private int amountCents;
    private String checkoutUrl;
}
