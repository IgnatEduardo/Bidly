package com.bidly.auctionservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "bidding_sessions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BiddingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @NotNull(message = "Reserve price is required")
    @Column(name = "reserve_price", nullable = false)
    private BigDecimal reservePrice;

    @Column(name = "buy_it_now_price")
    private BigDecimal buyItNowPrice;

    @NotNull(message = "Bid increment is required")
    @Column(name = "bid_increment", nullable = false)
    private BigDecimal bidIncrement;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @OneToMany(mappedBy = "biddingSession", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Bid> bids;
}
