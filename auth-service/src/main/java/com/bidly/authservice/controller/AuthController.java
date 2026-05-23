package com.bidly.authservice.controller;

import com.bidly.authservice.dto.RegisterRequest;
import com.bidly.authservice.dto.RegisterResponse;
import com.bidly.authservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest registerRequest
    ) {
        RegisterResponse registerResponse = authService.register(registerRequest);

        return new ResponseEntity<>(registerResponse, HttpStatus.CREATED);
    }

    @GetMapping("/confirm")
    public ResponseEntity<String> confirmAccount(
            @RequestParam("token") String token
    ) {
        String response = authService.confirmAccount(token);

        return ResponseEntity.ok(response);
    }


}
