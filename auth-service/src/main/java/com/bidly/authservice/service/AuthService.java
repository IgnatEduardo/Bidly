package com.bidly.authservice.service;

import com.bidly.authservice.dto.RegisterRequest;
import com.bidly.authservice.dto.RegisterResponse;
import com.bidly.authservice.repository.RoleRepository;
import com.bidly.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public RegisterResponse register(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            //throw new EmailAlreadyExistsException("Email already exists");
        }

        return null;
    }
}
