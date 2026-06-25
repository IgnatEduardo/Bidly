package com.bidly.authservice.service;

import com.bidly.authservice.entity.RefreshToken;
import com.bidly.authservice.entity.User;
import com.bidly.authservice.repository.RefreshTokenRepository;
import com.bidly.authservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(userRepository, refreshTokenRepository);
        // set refresh token expiration to 1 hour
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpiration", 3600_000L);
    }

    @Test
    void createRefreshToken_success() {
        User user = User.builder().id(7L).username("u").build();
        when(userRepository.findByUsername("u")).thenReturn(Optional.of(user));
        doNothing().when(refreshTokenRepository).deleteByUser(user);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken rt = refreshTokenService.createRefreshToken("u");

        assertNotNull(rt.getToken());
        assertTrue(rt.getExpiresDate().isAfter(Instant.now()));
        verify(refreshTokenRepository).deleteByUser(user);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void verifyExpiration_nonExpired_returnsToken() {
        RefreshToken t = RefreshToken.builder().expiresDate(Instant.now().plusSeconds(10)).build();
        RefreshToken result = refreshTokenService.verifyExpiration(t);
        assertSame(t, result);
    }

    @Test
    void verifyExpiration_expired_throws() {
        RefreshToken t = RefreshToken.builder().expiresDate(Instant.now().minusSeconds(10)).user(User.builder().username("u").build()).build();
        doNothing().when(refreshTokenRepository).delete(t);

        assertThrows(RuntimeException.class, () -> refreshTokenService.verifyExpiration(t));
        verify(refreshTokenRepository).delete(t);
    }

    @Test
    void deleteByUserId_success() {
        User user = User.builder().id(99L).username("u").build();
        when(userRepository.findById(99L)).thenReturn(Optional.of(user));
        doNothing().when(refreshTokenRepository).deleteByUser(user);

        refreshTokenService.deleteByUserId(99L);

        verify(refreshTokenRepository).deleteByUser(user);
    }

    @Test
    void findByToken_returnsOptional() {
        RefreshToken t = RefreshToken.builder().token("tok").build();
        when(refreshTokenRepository.findByToken("tok")).thenReturn(Optional.of(t));

        Optional<RefreshToken> res = refreshTokenService.findByToken("tok");
        assertTrue(res.isPresent());
        assertEquals("tok", res.get().getToken());
    }
}

