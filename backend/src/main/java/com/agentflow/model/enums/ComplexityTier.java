package com.agentflow.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ComplexityTier {
    SIMPLE(10,
        "Straightforward CRUD app with a clear, narrow scope.",
        "Basic data models, standard REST endpoints, minimal business logic."),

    MEDIUM(25,
        "Moderate complexity with multiple features and light integrations.",
        "Several domain entities, some workflow logic, one or two third-party services."),

    COMPLEX(50,
        "High complexity — cross-cutting concerns, multiple integrations, or non-trivial domain logic.",
        "Advanced business rules, multiple external APIs, real-time features, or security requirements."),

    ENTERPRISE(100,
        "Large-scale system with significant architectural complexity.",
        "Distributed components, event-driven architecture, multi-tenant design, or ML/AI workloads.");

    private final int creditCost;
    private final String summary;
    private final String rationale;
}
