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

    @Test
    void updateUser_returnsOk() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setPhoneNumber("0799999999");

        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .username("newuser")
                .email("new@example.com")
                .enabled(true)
                .kycApproved(true)
                .build();

        UpdateUserResponse response = UpdateUserResponse.builder()
                .user(userResponse)
                .accessToken("newAccessToken")
                .build();

        when(authService.updateUser(1L, request)).thenReturn(response);

        ResponseEntity<UpdateUserResponse> result = controller.updateUser(1L, request);

        assertEquals(200, result.getStatusCodeValue());
        assertNotNull(result.getBody());
        assertEquals("newuser", result.getBody().getUser().getUsername());
        assertEquals("new@example.com", result.getBody().getUser().getEmail());
        assertEquals("newAccessToken", result.getBody().getAccessToken());

        verify(authService).updateUser(1L, request);
    }

    @Test
    void deleteUser_returnsNoContent() {
        doNothing().when(authService).deleteUser(1L);

        ResponseEntity<Void> result = controller.deleteUser(1L);

        assertEquals(204, result.getStatusCodeValue());
        assertNull(result.getBody());

        verify(authService).deleteUser(1L);
    }
}

