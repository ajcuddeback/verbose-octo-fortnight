package com.agentflow.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    // JWT is in the HttpOnly cookie — never returned in the body
}
