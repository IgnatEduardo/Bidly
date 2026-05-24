package com.bidly.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class AuthResponse {
    private String token;

    private String type = "Bearer";

    private Long id;

    private String username;

    private String email;

    private List<String> roles;
}
