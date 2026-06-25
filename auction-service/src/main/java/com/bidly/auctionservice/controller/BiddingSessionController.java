package com.bidly.auctionservice.controller;

import com.bidly.auctionservice.dto.BiddingSessionRequest;
import com.bidly.auctionservice.dto.BiddingSessionResponse;
import com.bidly.auctionservice.service.AuctionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auctions/sessions")
@RequiredArgsConstructor
@Slf4j
public class BiddingSessionController {

    private final AuctionService auctionService;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public ResponseEntity<BiddingSessionResponse> getSession(@PathVariable Long id) {
        log.debug("GET getSession requested for id: {}", id);
        return ResponseEntity.ok(auctionService.getSessionById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public ResponseEntity<BiddingSessionResponse> updateSession(
            @PathVariable Long id,
            @Valid @RequestBody BiddingSessionRequest request
    ) {
        log.debug("PUT updateSession requested for id: {}", id);
        return ResponseEntity.ok(auctionService.updateSession(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public ResponseEntity<Void> deleteSession(@PathVariable Long id) {
        log.debug("DELETE deleteSession requested for id: {}", id);
        auctionService.deleteSession(id);
        return ResponseEntity.noContent().build();
    }
}
