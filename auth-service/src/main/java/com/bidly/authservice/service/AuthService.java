package com.bidly.authservice.service;

import com.bidly.authservice.dto.RegisterRequest;
import com.bidly.authservice.dto.RegisterResponse;
import com.bidly.authservice.entity.Role;
import com.bidly.authservice.entity.User;
import com.bidly.authservice.enums.Rolename;
import com.bidly.authservice.exception.classes.EmailAlreadyExistsException;
import com.bidly.authservice.repository.RoleRepository;
import com.bidly.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public RegisterResponse register(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        Role userRole = roleRepository.findByName(Rolename.USER.name())
                .orElseThrow(() -> new RuntimeException("User role not found"));

        User newUser = User.builder()
                .firstName(registerRequest.getFirstName())
                .lastName(registerRequest.getLastName())
                .username(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .enabled(false)
                .rolename(Set.of(userRole))
                .build();

        userRepository.save(newUser);

        //Generete verif token(email)




        return null;
    }
}
