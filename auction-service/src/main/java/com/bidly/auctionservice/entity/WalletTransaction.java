package com.bidly.auctionservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_transactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private UserWallet userWallet;

    @NotNull(message = "Transaction amount is required")
    @Column(nullable = false)
    private BigDecimal amount;

    @NotNull(message = "Transaction type is required")
    @Column(nullable = false)
    private String type; // e.g., "DEPOSIT", "LOCK", "RELEASE", "CHARGE"

    @NotNull(message = "Timestamp is required")
    @Column(nullable = false)
    private LocalDateTime timestamp;
}
