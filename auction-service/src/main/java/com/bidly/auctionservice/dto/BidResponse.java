package com.bidly.auctionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidResponse {
    private Long id;
    private Long biddingSessionId;
    private Long bidderId;
    private String bidderUsername;
    private BigDecimal amount;
    private LocalDateTime timestamp;
}
