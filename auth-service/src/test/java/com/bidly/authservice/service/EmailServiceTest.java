package com.bidly.authservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailServiceTest {
    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        emailService = new EmailService(mailSender);
    }

    @Test
    void sendConfirmationEmail_sendsMessage() {
        String to = "user@example.com";
        String link = "http://localhost/confirm?token=abc";

        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> emailService.sendConfirmationEmail(to, link));

        // capture and assert using ArgumentCaptor to avoid overload ambiguity
        org.mockito.ArgumentCaptor<SimpleMailMessage> captor = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertEquals(to, sent.getTo()[0]);
        assertTrue(sent.getSubject().contains("Confirmation"));
        assertTrue(sent.getText().contains(link));
    }

    @Test
    void sendConfirmationEmail_whenSenderThrows_exceptionPropagated() {
        String to = "x@y.com";
        String link = "lnk";

        doThrow(new RuntimeException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> emailService.sendConfirmationEmail(to, link));
        assertEquals("smtp down", ex.getMessage());
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}
