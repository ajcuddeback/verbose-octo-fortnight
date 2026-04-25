package com.agentflow.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.stripe")
public class StripeProperties {
    private String secretKey;
    private String webhookSecret;
    private String starterPriceId;
    private String proPriceId;
    private String pack100PriceId;
    private String pack500PriceId;
    private String pack1000PriceId;
}
