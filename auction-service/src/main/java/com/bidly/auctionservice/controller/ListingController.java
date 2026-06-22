package com.bidly.auctionservice.controller;

import com.bidly.auctionservice.dto.ListingRequest;
import com.bidly.auctionservice.dto.ListingResponse;
import com.bidly.auctionservice.service.AuctionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auctions/listings")
@RequiredArgsConstructor
public class ListingController {

    private final AuctionService auctionService;

    @PostMapping
    public ResponseEntity<ListingResponse> createListing(@Valid @RequestBody ListingRequest request) {
        ListingResponse response = auctionService.createListing(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ListingResponse> getListing(@PathVariable Long id) {
        return ResponseEntity.ok(auctionService.getListing(id));
    }

    @GetMapping
    public ResponseEntity<Page<ListingResponse>> getListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String[] sort
    ) {
        String sortField = sort[0];
        Sort.Direction sortDirection = Sort.Direction.fromString(sort.length > 1 ? sort[1] : "desc");
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortField));
        
        return ResponseEntity.ok(auctionService.getListings(pageable));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<ListingResponse> confirmSale(
            @PathVariable Long id,
            @RequestParam Long sellerId,
            @RequestParam boolean confirm
    ) {
        return ResponseEntity.ok(auctionService.confirmSale(id, sellerId, confirm));
    }

    @PostMapping("/{id}/checkout")
    public ResponseEntity<ListingResponse> finalizeCheckout(
            @PathVariable Long id,
            @RequestParam Long winnerId
    ) {
        return ResponseEntity.ok(auctionService.finalizeCheckout(id, winnerId));
    }

    @PostMapping("/{id}/forfeit")
    public ResponseEntity<Void> handleForfeiture(@PathVariable Long id) {
        auctionService.handleBuyerForfeiture(id);
        return ResponseEntity.ok().build();
    }
}
