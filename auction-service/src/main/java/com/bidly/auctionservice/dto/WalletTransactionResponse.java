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
public class WalletTransactionResponse {
    private Long id;
    private BigDecimal amount;
    private String type; // e.g. "DEPOSIT", "LOCK", "RELEASE", "CHARGE"
    private LocalDateTime timestamp;
}
