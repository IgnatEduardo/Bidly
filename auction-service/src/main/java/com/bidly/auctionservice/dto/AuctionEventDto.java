package com.bidly.auctionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuctionEventDto {
    private String type; // "BID_PLACED", "OUTBID", "AUCTION_ENDED"
    private String listingTitle;
    private String recipientEmail;
    private String recipientName;
    private BigDecimal amount;
    private String extraMessage;
}
