package com.bidly.authservice.service;

import com.bidly.authservice.dto.*;
import com.bidly.authservice.entity.Role;
import com.bidly.authservice.entity.User;
import com.bidly.authservice.enums.Rolename;
import com.bidly.authservice.exception.classes.EmailAlreadyExistsException;
import com.bidly.authservice.exception.classes.TokenExpiredException;
import com.bidly.authservice.exception.classes.UsernameAlreadyExistsException;
import com.bidly.authservice.repository.RefreshTokenRepository;
import com.bidly.authservice.repository.RoleRepository;
import com.bidly.authservice.repository.UserRepository;
import com.bidly.authservice.repository.VerificationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceIntegrationTest {
    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        // Mock email service to avoid SMTP connection
        doNothing().when(emailService).sendConfirmationEmail(anyString(), anyString());

        // Clear all data before each test (order matters due to foreign keys)
        refreshTokenRepository.deleteAll();
        verificationTokenRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        // Create USER role
        Role userRole = Role.builder().name(Rolename.USER).build();
        roleRepository.save(userRole);
    }

    @Test
    void testRegisterFlow() {
        RegisterRequest req = new RegisterRequest();
        req.setFirstName("John");
        req.setLastName("Doe");
        req.setUsername("johndoe");
        req.setEmail("john@example.com");
        req.setPassword("SecurePass123!");
        req.setPhoneNumber("0712345678");

        RegisterResponse resp = authService.register(req);

        assertNotNull(resp);
        assertEquals("john@example.com", resp.getEmail());
        assertTrue(resp.getMessage().contains("registered successfully"));

        // Verify user exists
        Optional<User> savedUser = userRepository.findByUsername("johndoe");
        assertTrue(savedUser.isPresent());
        assertFalse(savedUser.get().isEnabled()); // Still disabled until email confirmed
    }

    @Test
    void testRegisterDuplicate_EmailExists_ThrowsException() {
        // First registration
        RegisterRequest req1 = new RegisterRequest();
        req1.setUsername("user1");
        req1.setEmail("same@example.com");
        req1.setPassword("Pass123!");
        req1.setFirstName("First");
        req1.setLastName("Last");
        req1.setPhoneNumber("0712345678");

        authService.register(req1);

        // Try duplicate email
        RegisterRequest req2 = new RegisterRequest();
        req2.setUsername("user2");
        req2.setEmail("same@example.com");
        req2.setPassword("Pass456!");
        req2.setFirstName("Second");
        req2.setLastName("User");
        req2.setPhoneNumber("0787654321");

        assertThrows(EmailAlreadyExistsException.class, () -> authService.register(req2));
    }

    @Test
    void testRegisterDuplicate_UsernameExists_ThrowsException() {
        // First registration
        RegisterRequest req1 = new RegisterRequest();
        req1.setUsername("samename");
        req1.setEmail("first@example.com");
        req1.setPassword("Pass123!");
        req1.setFirstName("First");
        req1.setLastName("Last");
        req1.setPhoneNumber("0712345678");

        authService.register(req1);

        // Try duplicate username
        RegisterRequest req2 = new RegisterRequest();
        req2.setUsername("samename");
        req2.setEmail("second@example.com");
        req2.setPassword("Pass456!");
        req2.setFirstName("Second");
        req2.setLastName("User");
        req2.setPhoneNumber("0787654321");

        assertThrows(UsernameAlreadyExistsException.class, () -> authService.register(req2));
    }

    @Test
    void testConfirmAccount_Success() {
        // Register user
        RegisterRequest regReq = new RegisterRequest();
        regReq.setUsername("testuser");
        regReq.setEmail("test@example.com");
        regReq.setPassword("Pass123!");
        regReq.setFirstName("Test");
        regReq.setLastName("User");
        regReq.setPhoneNumber("0712345678");

        authService.register(regReq);

        // Get verification token
        Optional<User> user = userRepository.findByUsername("testuser");
        assertTrue(user.isPresent());
        assertFalse(user.get().isEnabled());

        var verificationToken = verificationTokenRepository.findAll().get(0);

        // Confirm account
        String result = authService.confirmAccount(verificationToken.getToken());
        assertEquals("Account confirmed successfully", result);

        // Verify user is enabled
        Optional<User> confirmedUser = userRepository.findByUsername("testuser");
        assertTrue(confirmedUser.isPresent());
        assertTrue(confirmedUser.get().isEnabled());
    }

    @Test
    void testConfirmAccount_ExpiredToken_ThrowsException() {
        // Register user
        RegisterRequest regReq = new RegisterRequest();
        regReq.setUsername("testuser2");
        regReq.setEmail("test2@example.com");
        regReq.setPassword("Pass123!");
        regReq.setFirstName("Test");
        regReq.setLastName("User");
        regReq.setPhoneNumber("0712345678");

        authService.register(regReq);

        // Get verification token and set it to expired
        var verificationToken = verificationTokenRepository.findAll().get(0);
        verificationToken.setExpiryDate(LocalDateTime.now().minusHours(1));
        verificationTokenRepository.save(verificationToken);

        // Try to confirm with expired token
        assertThrows(TokenExpiredException.class, () -> authService.confirmAccount(verificationToken.getToken()));
    }

    @Test
    void testLoginFlow() {
        // Register and confirm user
        RegisterRequest regReq = new RegisterRequest();
        regReq.setUsername("loginuser");
        regReq.setEmail("login@example.com");
        regReq.setPassword("MyPassword123!");
        regReq.setFirstName("Login");
        regReq.setLastName("User");
        regReq.setPhoneNumber("0712345678");

        authService.register(regReq);

        var verificationToken = verificationTokenRepository.findAll().get(0);
        authService.confirmAccount(verificationToken.getToken());

        // Login
        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsername("loginuser");
        loginReq.setPassword("MyPassword123!");

        LoginResponse loginResp = authService.login(loginReq);

        assertNotNull(loginResp);
        assertNotNull(loginResp.getAccessToken());
        assertNotNull(loginResp.getRefreshToken());
        assertEquals("loginuser", loginResp.getUsername());
        assertEquals("login@example.com", loginResp.getEmail());
    }

    @Test
    void testLogin_UserNotEnabled_ThrowsException() {
        // Register but don't confirm
        RegisterRequest regReq = new RegisterRequest();
        regReq.setUsername("unconfirmed");
        regReq.setEmail("unconf@example.com");
        regReq.setPassword("Pass123!");
        regReq.setFirstName("Un");
        regReq.setLastName("Confirmed");
        regReq.setPhoneNumber("0712345678");

        authService.register(regReq);

        // Try to login
        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsername("unconfirmed");
        loginReq.setPassword("Pass123!");

        assertThrows(RuntimeException.class, () -> authService.login(loginReq));
    }

    @Test
    void testLogin_InvalidPassword_ThrowsException() {
        // Register and confirm user
        RegisterRequest regReq = new RegisterRequest();
        regReq.setUsername("validuser");
        regReq.setEmail("valid@example.com");
        regReq.setPassword("CorrectPass123!");
        regReq.setFirstName("Valid");
        regReq.setLastName("User");
        regReq.setPhoneNumber("0712345678");

        authService.register(regReq);

        var verificationToken = verificationTokenRepository.findAll().get(0);
        authService.confirmAccount(verificationToken.getToken());

        // Try with wrong password
        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsername("validuser");
        loginReq.setPassword("WrongPassword");

        assertThrows(RuntimeException.class, () -> authService.login(loginReq));
    }

    @Test
    void testGetUserById() {
        // Register user
        RegisterRequest regReq = new RegisterRequest();
        regReq.setUsername("userbyid");
        regReq.setEmail("userid@example.com");
        regReq.setPassword("Pass123!");
        regReq.setFirstName("User");
        regReq.setLastName("ById");
        regReq.setPhoneNumber("0712345678");

        authService.register(regReq);

        var user = userRepository.findByUsername("userbyid").get();

        UserResponse userResp = authService.getUserById(user.getId());

        assertNotNull(userResp);
        assertEquals("userbyid", userResp.getUsername());
        assertEquals("userid@example.com", userResp.getEmail());
        assertFalse(userResp.getKycApproved());
    }

    @Test
    void testToggleKyc() {
        // Register user
        RegisterRequest regReq = new RegisterRequest();
        regReq.setUsername("kycuser");
        regReq.setEmail("kyc@example.com");
        regReq.setPassword("Pass123!");
        regReq.setFirstName("KYC");
        regReq.setLastName("User");
        regReq.setPhoneNumber("0712345678");

        authService.register(regReq);

        var user = userRepository.findByUsername("kycuser").get();

        // Toggle KYC to true
        UserResponse toggleResp = authService.toggleKyc(user.getId(), true);
        assertTrue(toggleResp.getKycApproved());

        // Verify in DB
        Optional<User> verified = userRepository.findById(user.getId());
        assertTrue(verified.isPresent());
        assertTrue(verified.get().getKycApproved());
    }
}

