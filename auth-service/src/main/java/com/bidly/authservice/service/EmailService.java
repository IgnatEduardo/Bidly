package com.bidly.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    public void sendConfirmationEmail(String to, String link) {
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(to);
            mailMessage.setSubject("Confirmation Link");
            mailMessage.setText("Welcome to Bidly! Please click the following link to confirm your account: " + link);
            mailSender.send(mailMessage);

            log.info("Confirmation email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send notificaiton email to {}", to, e);
            throw e;
        }
    }
}
