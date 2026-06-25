package com.bidly.authservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateUserResponse {
    private UserResponse user;
    private String accessToken;
}
