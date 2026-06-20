package com.bidly.authservice.service;

import com.bidly.authservice.config.JwtService;
import com.bidly.authservice.dto.*;
import com.bidly.authservice.entity.RefreshToken;
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
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final RefreshTokenService refreshTokenService;

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
            user.setEnabled(true);
            userRepository.save(user);
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        var accessToken = jwtService.generateToken(user);
        var refreshToken = refreshTokenService.createRefreshToken(user.getUsername());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .username(user.getUsername())
                .email(user.getEmail())
                .id(user.getId())
                .kycApproved(user.getKycApproved() != null ? user.getKycApproved() : false)
                .build();
    }

    public TokenRefreshResponse refreshToken(TokenRefreshRequest refreshTokenRequest) {
        return refreshTokenService.findByToken(refreshTokenRequest.getRefreshToken())
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String accessToken = jwtService.generateToken(user);

                    return TokenRefreshResponse.builder()
                            .accessToken(accessToken)
                            .refreshToken(refreshTokenRequest.getRefreshToken())
                            .build();
                })
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database"));
    }

    @Transactional
    public void logout(User user) {
        refreshTokenService.deleteByUserId(user.getId());

        SecurityContextHolder.clearContext();
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .enabled(user.isEnabled())
                .kycApproved(user.getKycApproved())
                .build();
    }

    @Transactional
    public UserResponse toggleKyc(Long id, boolean kycApproved) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        user.setKycApproved(kycApproved);
        user = userRepository.save(user);
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .enabled(user.isEnabled())
                .kycApproved(user.getKycApproved())
                .build();
    }
}












