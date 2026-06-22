package com.bidly.auctionservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bids")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bidding_session_id", nullable = false)
    private BiddingSession biddingSession;

    @NotNull(message = "Bidder ID is required")
    @Column(name = "bidder_id", nullable = false)
    private Long bidderId;

    @NotNull(message = "Bid amount is required")
    @Column(nullable = false)
    private BigDecimal amount;

    @NotNull(message = "Timestamp is required")
    @Column(nullable = false)
    private LocalDateTime timestamp;
}
