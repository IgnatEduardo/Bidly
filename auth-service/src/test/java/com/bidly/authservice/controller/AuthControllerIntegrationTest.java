package com.bidly.authservice.controller;

import com.bidly.authservice.dto.*;
import com.bidly.authservice.entity.Role;
import com.bidly.authservice.enums.Rolename;
import com.bidly.authservice.repository.RefreshTokenRepository;
import com.bidly.authservice.repository.RoleRepository;
import com.bidly.authservice.repository.UserRepository;
import com.bidly.authservice.repository.VerificationTokenRepository;
import com.bidly.authservice.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private ObjectMapper objectMapper;

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

        Role userRole = Role.builder().name(Rolename.USER).build();
        roleRepository.save(userRole);
    }

    @Test
    void testRegisterEndpoint_Success() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setFirstName("John");
        req.setLastName("Doe");
        req.setUsername("johndoe");
        req.setEmail("john@example.com");
        req.setPassword("SecurePass123!");
        req.setPhoneNumber("0712345678");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.message").value(containsString("registered successfully")));
    }

    @Test
    void testRegisterEndpoint_DuplicateEmail_Conflict() throws Exception {
        // First registration
        RegisterRequest req1 = new RegisterRequest();
        req1.setFirstName("First");
        req1.setLastName("User");
        req1.setUsername("user1");
        req1.setEmail("email@example.com");
        req1.setPassword("Pass123!");
        req1.setPhoneNumber("0712345678");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        // Duplicate email
        RegisterRequest req2 = new RegisterRequest();
        req2.setFirstName("Second");
        req2.setLastName("User");
        req2.setUsername("user2");
        req2.setEmail("email@example.com");
        req2.setPassword("Pass456!");
        req2.setPhoneNumber("0787654321");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void testLoginEndpoint_Success() throws Exception {
        // Setup: Register and confirm
        RegisterRequest regReq = new RegisterRequest();
        regReq.setFirstName("Login");
        regReq.setLastName("User");
        regReq.setUsername("loginuser");
        regReq.setEmail("login@example.com");
        regReq.setPassword("MyPass123!");
        regReq.setPhoneNumber("0712345678");

        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated())
                .andReturn();

        // Confirm account
        var verToken = verificationTokenRepository.findAll().get(0);
        mockMvc.perform(get("/api/v1/auth/confirm")
                        .param("token", verToken.getToken()))
                .andExpect(status().isOk());

        // Login
        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsername("loginuser");
        loginReq.setPassword("MyPass123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.username").value("loginuser"))
                .andExpect(jsonPath("$.email").value("login@example.com"));
    }

    @Test
    void testConfirmEndpoint_WithValidToken() throws Exception {
        // Register first
        RegisterRequest regReq = new RegisterRequest();
        regReq.setFirstName("Confirm");
        regReq.setLastName("Test");
        regReq.setUsername("confirmtest");
        regReq.setEmail("confirm@example.com");
        regReq.setPassword("Pass123!");
        regReq.setPhoneNumber("0712345678");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated());

        // Get token and confirm
        var verToken = verificationTokenRepository.findAll().get(0);

        mockMvc.perform(get("/api/v1/auth/confirm")
                        .param("token", verToken.getToken()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("confirmed successfully")));
    }

//    @Test
//    void testGetUserEndpoint() throws Exception {
//        // Register user
//        RegisterRequest regReq = new RegisterRequest();
//        regReq.setFirstName("Get");
//        regReq.setLastName("User");
//        regReq.setUsername("getuser");
//        regReq.setEmail("get@example.com");
//        regReq.setPassword("Pass123!");
//        regReq.setPhoneNumber("0712345678");
//
//        mockMvc.perform(post("/api/v1/auth/register")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(regReq)))
//                .andExpect(status().isCreated());
//
//        var user = userRepository.findByUsername("getuser").get();
//
//        mockMvc.perform(get("/api/v1/auth/users/{id}", user.getId()))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.username").value("getuser"))
//                .andExpect(jsonPath("$.email").value("get@example.com"));
//    }

    @Test
    void testToggleKycEndpoint() throws Exception {
        // Register user
        RegisterRequest regReq = new RegisterRequest();
        regReq.setFirstName("KYC");
        regReq.setLastName("Test");
        regReq.setUsername("kyctest");
        regReq.setEmail("kyc@example.com");
        regReq.setPassword("Pass123!");
        regReq.setPhoneNumber("0712345678");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated());

        var user = userRepository.findByUsername("kyctest").get();

        mockMvc.perform(post("/api/v1/auth/users/{id}/kyc", user.getId())
                        .param("approved", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kycApproved").value(true));
    }

    @Test
    void testLoginEndpoint_WithWrongPassword() throws Exception {
        // Setup: Register
        RegisterRequest regReq = new RegisterRequest();
        regReq.setFirstName("Wrong");
        regReq.setLastName("Pass");
        regReq.setUsername("wrongpass");
        regReq.setEmail("wrong@example.com");
        regReq.setPassword("CorrectPass123!");
        regReq.setPhoneNumber("0712345678");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated());

        // Confirm
        var verToken = verificationTokenRepository.findAll().get(0);
        mockMvc.perform(get("/api/v1/auth/confirm")
                        .param("token", verToken.getToken()))
                .andExpect(status().isOk());

        // Try login with wrong password
        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsername("wrongpass");
        loginReq.setPassword("WrongPassword");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isInternalServerError());
    }
}

