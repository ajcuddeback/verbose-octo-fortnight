package com.agentflow.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.credits")
public class CreditProperties {
    private int freeMonthlyAllowance = 50;
    private int maxWorkflowsPerHour = 5;
    private int starterMonthlyAllowance = 500;
    private int proMonthlyAllowance = 2000;
}
