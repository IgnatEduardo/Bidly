package com.bidly.authservice.service;

import com.bidly.authservice.dto.RegisterRequest;
import com.bidly.authservice.dto.RegisterResponse;
import com.bidly.authservice.entity.Role;
import com.bidly.authservice.entity.User;
import com.bidly.authservice.entity.VerificationToken;
import com.bidly.authservice.enums.Rolename;
import com.bidly.authservice.exception.classes.EmailAlreadyExistsException;
import com.bidly.authservice.exception.classes.TokenExpiredException;
import com.bidly.authservice.repository.RoleRepository;
import com.bidly.authservice.repository.UserRepository;
import com.bidly.authservice.repository.VerificationTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenRepository verificationTokenRepository;
    private final EmailService emailService;

    @Transactional
    public RegisterResponse register(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        Role userRole = roleRepository.findByName(Rolename.USER)
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
        String codUnic =  UUID.randomUUID().toString();
        VerificationToken verificationToken = VerificationToken.builder()
                .token(codUnic)
                .user(newUser)
                .expiryDate(LocalDateTime.now().plusHours(12))
                .build();

        verificationTokenRepository.save(verificationToken);

        String confirmationLink = "http://localhost:8080/api/v1/auth/confirm?token=" + codUnic;
        emailService.sendConfirmationEmail(newUser.getEmail(), confirmationLink);

        return RegisterResponse.builder()
                .message("User registered successfully. Please check your email to confirm your account.")
                .email(newUser.getEmail())
                .build();
    }

    @Transactional
    public String confirmAccount(String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid verification token"));

        if (verificationToken.isExpired()) {
            verificationTokenRepository.delete(verificationToken);
            throw new TokenExpiredException("Token expired");
        }

        User user = verificationToken.getUser();
        user.setEnabled(true);
        userRepository.save(user);

        verificationTokenRepository.delete(verificationToken);

        return "Account confirmed successfully";
    }
}












