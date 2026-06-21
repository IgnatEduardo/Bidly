package com.bidly.auctionservice.controller;

import com.bidly.auctionservice.dto.BidRequest;
import com.bidly.auctionservice.dto.BidResponse;
import com.bidly.auctionservice.service.AuctionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auctions/listings/{listingId}/bids")
@RequiredArgsConstructor
@Slf4j
public class BidController {

    private final AuctionService auctionService;

    @PostMapping
    public ResponseEntity<BidResponse> placeBid(
            @PathVariable Long listingId,
            @Valid @RequestBody BidRequest request
    ) {
        log.debug("POST placeBid requested for listingId: {}, amount: {}, bidderId: {}", listingId, request.getAmount(), request.getBidderId());
        BidResponse response = auctionService.placeBid(listingId, request);
        log.info("Successfully placed bid of {} on listing {} by bidder {}", request.getAmount(), listingId, request.getBidderId());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<BidResponse>> getBids(
            @PathVariable Long listingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "amount,desc") String[] sort
    ) {
        log.debug("GET getBids requested for listingId: {}, page: {}, size: {}, sort: {}", listingId, page, size, sort);
        String sortField = sort[0];
        Sort.Direction sortDirection = Sort.Direction.fromString(sort.length > 1 ? sort[1] : "desc");
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortField));
        Page<BidResponse> bids = auctionService.getBidsForListing(listingId, pageable);
        log.info("Fetched page {} of bids for listing {}, found {} elements", page, listingId, bids.getNumberOfElements());
        return ResponseEntity.ok(bids);
    }
}
