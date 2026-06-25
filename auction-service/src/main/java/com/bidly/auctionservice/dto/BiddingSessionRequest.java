package com.bidly.auctionservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BiddingSessionRequest {

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    @NotNull(message = "Reserve price is required")
    @DecimalMin(value = "0.01", message = "Reserve price must be greater than 0")
    private BigDecimal reservePrice;

    private BigDecimal buyItNowPrice;

    @NotNull(message = "Bid increment is required")
    @DecimalMin(value = "0.01", message = "Bid increment must be greater than 0")
    private BigDecimal bidIncrement;
}
