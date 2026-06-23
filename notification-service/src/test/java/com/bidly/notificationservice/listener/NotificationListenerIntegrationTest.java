package com.bidly.notificationservice.listener;

import com.bidly.notificationservice.dto.AuctionEventDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.ArgumentCaptor;

@SpringBootTest
class NotificationListenerIntegrationTest {

    @Autowired
    private NotificationListener listener;

    @MockBean
    private JavaMailSender mailSender;

    @Test
    void contextLoads_andListenerBeanAvailable() {
        assertThat(listener).isNotNull();
    }

    @Test
    void whenAuctionEnded_shouldSendEmailContainingExtraMessage() {
        AuctionEventDto event = AuctionEventDto.builder()
                .type("AUCTION_ENDED")
                .listingTitle("Antique Vase")
                .recipientEmail("winner@example.com")
                .recipientName("Daniel")
                .amount(new BigDecimal("999.99"))
                .extraMessage("Congrats, you won!")
                .build();

        listener.handleAuctionEvent(event);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getSubject()).contains("Auction Ended").contains("Antique Vase");
        assertThat(sent.getText()).contains("Congrats, you won!").contains("Final Price: $999.99");
    }
}

