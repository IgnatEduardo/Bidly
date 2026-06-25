package com.bidly.authservice.service;

import com.bidly.authservice.entity.RefreshToken;
import com.bidly.authservice.repository.RefreshTokenRepository;
import com.bidly.authservice.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    @Value("${application.security.jwt.refresh-token.expiration}")
    private long refreshTokenExpiration;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshToken createRefreshToken(String username) {
        log.info("Creating refresh token for user {}", username);

        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        refreshTokenRepository.deleteByUser(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiresDate(Instant.now().plusMillis(refreshTokenExpiration))
                .build();

        log.info("Created refresh token for user {}", username);

        return  refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiresDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            log.warn("Expired refresh token for user {}", token.getUser().getUsername());
            throw new RuntimeException("Refresh token was expired. Please make a new signin request");
        }

        return token;
    }

    @Transactional
    public void deleteByUserId(Long userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() ->  {
                    log.warn("Logout failed, user not found id={}", userId);
                    return new RuntimeException("User not found");
                });

        refreshTokenRepository.deleteByUser(user);

        log.info("Refresh token deleted for user {}", userId);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }
}
