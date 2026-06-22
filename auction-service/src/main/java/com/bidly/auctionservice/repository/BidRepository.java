package com.bidly.auctionservice.repository;

import com.bidly.auctionservice.entity.Bid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {
    List<Bid> findByBiddingSessionIdOrderByAmountDesc(Long biddingSessionId);
    Page<Bid> findByBiddingSessionId(Long biddingSessionId, Pageable pageable);
    List<Bid> findByBidderId(Long bidderId);
    Page<Bid> findByBidderId(Long bidderId, Pageable pageable);
}
