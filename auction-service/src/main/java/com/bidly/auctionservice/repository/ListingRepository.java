package com.bidly.auctionservice.repository;

import com.bidly.auctionservice.entity.Listing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Long> {
    List<Listing> findBySellerId(Long sellerId);
}
