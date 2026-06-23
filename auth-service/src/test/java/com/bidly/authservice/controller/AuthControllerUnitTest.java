package com.bidly.authservice.controller;

import com.bidly.authservice.dto.*;
import com.bidly.authservice.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthControllerUnitTest {
    private AuthController controller;

    @Mock
    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new AuthController(authService);
    }

    @Test
    void register_returnsCreated() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("a@b");
        req.setUsername("u");
        RegisterResponse resp = RegisterResponse.builder().email("a@b").message("ok").build();
        when(authService.register(req)).thenReturn(resp);

        ResponseEntity<RegisterResponse> result = controller.register(req);
        assertEquals(201, result.getStatusCodeValue());
        assertEquals("a@b", result.getBody().getEmail());
    }

    @Test
    void confirmAccount_returnsOk() {
        when(authService.confirmAccount("t")).thenReturn("confirmed");
        ResponseEntity<String> res = controller.confirmAccount("t");
        assertEquals(200, res.getStatusCodeValue());
        assertEquals("confirmed", res.getBody());
    }

    @Test
    void login_returnsOk() {
        LoginRequest req = new LoginRequest();
        req.setUsername("u"); req.setPassword("p");
        LoginResponse lr = LoginResponse.builder().accessToken("tok").username("u").build();
        when(authService.login(req)).thenReturn(lr);

        ResponseEntity<LoginResponse> res = controller.login(req);
        assertEquals(200, res.getStatusCodeValue());
        assertEquals("tok", res.getBody().getAccessToken());
    }

    @Test
    void refreshToken_returnsOk() {
        TokenRefreshRequest tr = new TokenRefreshRequest();
        tr.setRefreshToken("r");
        TokenRefreshResponse trr = TokenRefreshResponse.builder().accessToken("a").refreshToken("r").build();
        when(authService.refreshToken(tr)).thenReturn(trr);

        ResponseEntity<TokenRefreshResponse> res = controller.refreshToken(tr);
        assertEquals(200, res.getStatusCodeValue());
        assertEquals("r", res.getBody().getRefreshToken());
    }

    @Test
    void logout_returnsOk() {
        doNothing().when(authService).logout(any());

        ResponseEntity<String> res = controller.logout(null);
        assertEquals(200, res.getStatusCodeValue());
        assertEquals("Logout successful", res.getBody());
    }

    @Test
    void toggleKyc_and_getUser() {
        UserResponse ur = UserResponse.builder().id(2L).username("u").email("e").enabled(true).kycApproved(true).build();
        when(authService.toggleKyc(2L, true)).thenReturn(ur);
        when(authService.getUserById(2L)).thenReturn(ur);

        ResponseEntity<UserResponse> r1 = controller.toggleKyc(2L, true);
        assertEquals(200, r1.getStatusCodeValue());
        assertTrue(r1.getBody().getKycApproved());

        ResponseEntity<UserResponse> r2 = controller.getUser(2L);
        assertEquals(200, r2.getStatusCodeValue());
        assertEquals("u", r2.getBody().getUsername());
    }
}

