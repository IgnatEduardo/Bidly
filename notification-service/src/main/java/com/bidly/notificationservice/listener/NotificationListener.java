package com.bidly.notificationservice.listener;

import com.bidly.notificationservice.config.RabbitMQConfig;
import com.bidly.notificationservice.dto.AuctionEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final JavaMailSender mailSender;

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void handleAuctionEvent(AuctionEventDto event) {
        log.info("Received RabbitMQ event: type={}, listing={}, recipient={}, amount={}", 
                event.getType(), event.getListingTitle(), event.getRecipientEmail(), event.getAmount());

        String subject = "";
        String body = "";

        switch (event.getType()) {
            case "BID_PLACED":
                subject = "Bid Confirmed: " + event.getListingTitle();
                body = String.format("Hello %s,\n\nYour bid of $%s was placed successfully on listing \"%s\".\n\nBest regards,\nBidly Team",
                        event.getRecipientName(), event.getAmount(), event.getListingTitle());
                break;
            case "OUTBID":
                subject = "Outbid Alert: " + event.getListingTitle();
                body = String.format("Hello %s,\n\nYou have been outbid on listing \"%s\". The new highest bid is $%s.\n\nGo to Bidly to place a higher bid!\n\nBest regards,\nBidly Team",
                        event.getRecipientName(), event.getListingTitle(), event.getAmount());
                break;
            case "AUCTION_ENDED":
                subject = "Auction Ended: " + event.getListingTitle();
                body = String.format("Hello %s,\n\nThe auction for \"%s\" has closed.\nFinal Price: $%s.\n\nStatus details: %s\n\nBest regards,\nBidly Team",
                        event.getRecipientName(), event.getListingTitle(), event.getAmount(), event.getExtraMessage());
                break;
            default:
                log.warn("Unknown event type: {}", event.getType());
                return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(event.getRecipientEmail());
            message.setSubject(subject);
            message.setText(body);
            message.setFrom("no-reply@bidly.com");

            mailSender.send(message);
            log.info("Successfully sent notification email to {}", event.getRecipientEmail());
        } catch (Exception e) {
            log.error("Failed to send email to {} due to connection error. Logged body:\n---[EMAIL START]---\nSubject: {}\nTo: {}\nBody: {}\n---[EMAIL END]---",
                    event.getRecipientEmail(), subject, event.getRecipientEmail(), body);
        }
    }
}
