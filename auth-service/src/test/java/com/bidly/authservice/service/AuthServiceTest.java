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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private VerificationTokenRepository verificationTokenRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, roleRepository, passwordEncoder,
                verificationTokenRepository, emailService, jwtService, refreshTokenService);
    }

    @Test
    void register_success() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("jdoe");
        req.setEmail("jdoe@example.com");
        req.setPassword("plain");
        req.setFirstName("John");
        req.setLastName("Doe");
        req.setPhoneNumber("123");

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(req.getUsername())).thenReturn(false);
        when(roleRepository.findByName(Rolename.USER)).thenReturn(Optional.of(Role.builder().name(Rolename.USER).build()));
        when(passwordEncoder.encode(any())).thenReturn("encoded");

        // simulate save returning same user with id set
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(42L);
            return u;
        });

        RegisterResponse resp = authService.register(req);

        assertNotNull(resp);
        assertEquals(req.getEmail(), resp.getEmail());
        verify(verificationTokenRepository).save(any(VerificationToken.class));
        verify(emailService).sendConfirmationEmail(eq(req.getEmail()), contains("token="));
    }

    @Test
    void register_emailExists_throws() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("a@b");
        req.setUsername("u");
        when(userRepository.existsByEmail(req.getEmail())).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> authService.register(req));
    }

    @Test
    void register_usernameExists_throws() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("a@b");
        req.setUsername("u");
        when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(req.getUsername())).thenReturn(true);

        assertThrows(UsernameAlreadyExistsException.class, () -> authService.register(req));
    }

    @Test
    void confirmAccount_success() {
        User user = User.builder().id(1L).username("u").enabled(false).build();
        VerificationToken token = VerificationToken.builder()
                .token("t")
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(1))
                .build();

        when(verificationTokenRepository.findByToken("t")).thenReturn(Optional.of(token));

        String result = authService.confirmAccount("t");

        assertEquals("Account confirmed successfully", result);
        assertTrue(user.isEnabled());
        verify(userRepository).save(user);
        verify(verificationTokenRepository).delete(token);
    }

    @Test
    void confirmAccount_expired_throws() {
        User user = User.builder().id(1L).username("u").enabled(false).build();
        VerificationToken token = VerificationToken.builder()
                .token("t")
                .user(user)
                .expiryDate(LocalDateTime.now().minusHours(1))
                .build();

        when(verificationTokenRepository.findByToken("t")).thenReturn(Optional.of(token));

        assertThrows(TokenExpiredException.class, () -> authService.confirmAccount("t"));
        verify(verificationTokenRepository).delete(token);
    }

    @Test
    void login_success() {
        LoginRequest req = new LoginRequest();
        req.setUsername("u");
        req.setPassword("p");
        User user = User.builder().id(5L).username("u").password("encoded").email("e@e.com").enabled(true).build();

        when(userRepository.findByUsername("u")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("p", "encoded")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("tok");
        when(refreshTokenService.createRefreshToken(user.getUsername())).thenReturn(RefreshToken.builder().token("r").build());

        LoginResponse resp = authService.login(req);

        assertEquals("tok", resp.getAccessToken());
        assertEquals("r", resp.getRefreshToken());
        assertEquals(user.getUsername(), resp.getUsername());
    }

    @Test
    void login_notEnabled_throws() {
        LoginRequest req = new LoginRequest();
        req.setUsername("u");
        req.setPassword("p");
        User user = User.builder().id(5L).username("u").password("encoded").enabled(false).build();
        when(userRepository.findByUsername("u")).thenReturn(Optional.of(user));

        assertThrows(RuntimeException.class, () -> authService.login(req));
    }

    @Test
    void getUserById_and_toggleKyc() {
        User user = User.builder().id(100L).username("ux").email("a@a").enabled(true).kycApproved(false).build();
        when(userRepository.findById(100L)).thenReturn(Optional.of(user));

        var resp = authService.getUserById(100L);
        assertEquals(user.getUsername(), resp.getUsername());
        assertFalse(resp.getKycApproved());

        User saved = User.builder().id(100L).username("ux").email("a@a").enabled(true).kycApproved(true).build();
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(userRepository.findById(100L)).thenReturn(Optional.of(user));

        var kycResp = authService.toggleKyc(100L, true);
        assertTrue(kycResp.getKycApproved());
    }
}


