package com.bidly.auctionservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "user_wallets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "User ID is required")
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @NotNull(message = "Balance is required")
    @Column(nullable = false)
    private BigDecimal balance;

    @NotNull(message = "Locked balance is required")
    @Column(name = "locked_balance", nullable = false)
    private BigDecimal lockedBalance;

    @OneToMany(mappedBy = "userWallet", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<WalletTransaction> transactions;
}
