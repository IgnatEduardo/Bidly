package com.bidly.auctionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponse {
    private Long userId;
    private BigDecimal balance;
    private BigDecimal lockedBalance;
    private List<WalletTransactionResponse> transactions;
}
