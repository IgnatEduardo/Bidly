package com.bidly.notificationservice.listener;

import com.bidly.notificationservice.dto.AuctionEventDto;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class NotificationListenerUnitTest {

    @Test
    void whenBidPlaced_shouldSendEmailWithExpectedSubjectAndBody() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        NotificationListener listener = new NotificationListener(mailSender);

        AuctionEventDto event = AuctionEventDto.builder()
                .type("BID_PLACED")
                .listingTitle("Vintage Clock")
                .recipientEmail("user@example.com")
                .recipientName("Alice")
                .amount(new BigDecimal("123.45"))
                .build();

        listener.handleAuctionEvent(event);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getTo()).containsExactly("user@example.com");
        assertThat(sent.getFrom()).isEqualTo("no-reply@bidly.com");
        assertThat(sent.getSubject()).contains("Bid Confirmed").contains("Vintage Clock");
        assertThat(sent.getText()).contains("Your bid of $123.45").contains("Hello Alice");
    }

    @Test
    void whenUnknownType_shouldNotSendEmail() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        NotificationListener listener = new NotificationListener(mailSender);

        AuctionEventDto event = AuctionEventDto.builder()
                .type("UNKNOWN")
                .listingTitle("X")
                .recipientEmail("user@example.com")
                .recipientName("Bob")
                .amount(new BigDecimal("1"))
                .build();

        listener.handleAuctionEvent(event);

        verify(mailSender, times(0)).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
    }

    @Test
    void whenMailSenderThrows_exceptionIsHandled() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        doThrow(new RuntimeException("smtp down")).when(mailSender).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));

        NotificationListener listener = new NotificationListener(mailSender);

        AuctionEventDto event = AuctionEventDto.builder()
                .type("OUTBID")
                .listingTitle("Item")
                .recipientEmail("user2@example.com")
                .recipientName("Carol")
                .amount(new BigDecimal("50"))
                .build();

        // Should not throw despite mailSender throwing
        listener.handleAuctionEvent(event);
    }
}

