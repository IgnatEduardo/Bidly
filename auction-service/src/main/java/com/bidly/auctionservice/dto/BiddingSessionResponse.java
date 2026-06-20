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
public class BiddingSessionResponse {
    private Long id;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal reservePrice;
    private BigDecimal buyItNowPrice;
    private BigDecimal bidIncrement;
    private Boolean active;
    private BigDecimal currentHighestBid;
}
