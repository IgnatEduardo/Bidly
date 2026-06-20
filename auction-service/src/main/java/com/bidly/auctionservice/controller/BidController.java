package com.bidly.auctionservice.controller;

import com.bidly.auctionservice.dto.BidRequest;
import com.bidly.auctionservice.dto.BidResponse;
import com.bidly.auctionservice.service.AuctionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auctions/listings/{listingId}/bids")
@RequiredArgsConstructor
public class BidController {

    private final AuctionService auctionService;

    @PostMapping
    public ResponseEntity<BidResponse> placeBid(
            @PathVariable Long listingId,
            @Valid @RequestBody BidRequest request
    ) {
        BidResponse response = auctionService.placeBid(listingId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
