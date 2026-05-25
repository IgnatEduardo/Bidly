package com.bidly.authservice.service;

import com.bidly.authservice.config.JwtService;
import com.bidly.authservice.dto.LoginRequest;
import com.bidly.authservice.dto.LoginResponse;
import com.bidly.authservice.dto.RegisterRequest;
import com.bidly.authservice.dto.RegisterResponse;
import com.bidly.authservice.entity.Role;
import com.bidly.authservice.entity.User;
import com.bidly.authservice.entity.VerificationToken;
import com.bidly.authservice.enums.Rolename;
import com.bidly.authservice.exception.classes.EmailAlreadyExistsException;
import com.bidly.authservice.exception.classes.TokenExpiredException;
import com.bidly.authservice.exception.classes.UsernameAlreadyExistsException;
import com.bidly.authservice.repository.RoleRepository;
import com.bidly.authservice.repository.UserRepository;
import com.bidly.authservice.repository.VerificationTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
    private final JwtService jwtService;

    @Transactional
    public RegisterResponse register(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new UsernameAlreadyExistsException("Username already exists");
        }

        Role userRole = roleRepository.findByName(Rolename.USER)
                .orElseThrow(() -> new RuntimeException("User role not found"));

        User newUser = User.builder()
                .firstName(registerRequest.getFirstName())
                .lastName(registerRequest.getLastName())
                .username(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .phoneNumber(registerRequest.getPhoneNumber())
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

        String confirmationLink = "http://localhost:8081/api/v1/auth/confirm?token=" + codUnic;
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

    public LoginResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isEnabled()) {
            throw new RuntimeException("Please confirm your email before logging in.");
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        String jwtToken = jwtService.generateToken(user);

        return LoginResponse.builder()
                .token(jwtToken)
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }
}












