package com.bidly.auctionservice.service;

import com.bidly.auctionservice.dto.*;
import com.bidly.auctionservice.entity.Bid;
import com.bidly.auctionservice.entity.BiddingSession;
import com.bidly.auctionservice.entity.Listing;
import com.bidly.auctionservice.entity.UserWallet;
import com.bidly.auctionservice.exception.classes.InvalidBidException;
import com.bidly.auctionservice.exception.classes.ResourceNotFoundException;
import com.bidly.auctionservice.repository.BidRepository;
import com.bidly.auctionservice.repository.BiddingSessionRepository;
import com.bidly.auctionservice.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bidly.auctionservice.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.bidly.auctionservice.client.UserClient;
import com.bidly.auctionservice.dto.UserDto;
import com.bidly.auctionservice.config.AuctionWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionService {

    private final ListingRepository listingRepository;
    private final BiddingSessionRepository sessionRepository;
    private final BidRepository bidRepository;
    private final WalletService walletService;
    private final UserClient userClient;
    private final RabbitTemplate rabbitTemplate;
    private final AuctionWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    private static final BigDecimal ESCROW_RATE = new BigDecimal("0.10"); // 10% deposit required

    private boolean isHighValueCategory(String category) {
        if (category == null) return false;
        String lower = category.toLowerCase().trim();
        return lower.equals("real estate") || lower.equals("vehicles") 
                || lower.equals("imobiliare") || lower.equals("auto")
                || lower.equals("car") || lower.equals("house");
    }

    @Transactional
    public ListingResponse createListing(ListingRequest request) {
        LocalDateTime now = LocalDateTime.now();
        // Enforce start date must be starting now/future (with a small 1-minute buffer for latency)
        if (request.getStartTime().isBefore(now.minusMinutes(1))) {
            throw new IllegalArgumentException("Start time must be starting now or in the future.");
        }
        // Enforce end time is after start time
        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new IllegalArgumentException("End time must be after the start time.");
        }
        // Enforce end time is at most 6 months from the start time
        if (request.getEndTime().isAfter(request.getStartTime().plusMonths(6))) {
            throw new IllegalArgumentException("End time must be at most 6 months from the start time.");
        }

        // Enforce KYC check for high-value categories
        if (isHighValueCategory(request.getCategory())) {
            UserDto seller = userClient.getUserById(request.getSellerId());
            if (seller == null || !Boolean.TRUE.equals(seller.getKycApproved())) {
                throw new IllegalArgumentException("Sellers must be KYC-Approved to create listings in high-value categories like real estate or vehicles.");
            }
        }

        Listing listing = Listing.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .category(request.getCategory())
                .sellerId(request.getSellerId())
                .confirmed(false)
                .paid(false)
                .build();

        listing = listingRepository.save(listing);

        BiddingSession session = BiddingSession.builder()
                .listing(listing)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .reservePrice(request.getReservePrice())
                .buyItNowPrice(request.getBuyItNowPrice())
                .bidIncrement(request.getBidIncrement())
                .active(true)
                .build();

        session = sessionRepository.save(session);
        listing.setBiddingSession(session);

        return mapToListingResponse(listing);
    }

    public Page<ListingResponse> getListings(Pageable pageable) {
        log.debug("Entering getListings with pageable: {}", pageable);
        return listingRepository.findAll(pageable).map(this::mapToListingResponse);
    }

    public ListingResponse getListing(Long id) {
        log.debug("Entering getListing for id: {}", id);
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + id));
        return mapToListingResponse(listing);
    }

    @Transactional(readOnly = true)
    public Page<BidResponse> getBidsForListing(Long listingId, Pageable pageable) {
        log.debug("Entering getBidsForListing for listingId: {}, pageable: {}", listingId, pageable);
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + listingId));
        
        BiddingSession session = listing.getBiddingSession();
        if (session == null) {
            log.info("No bidding session found for listing: {}, returning empty page of bids", listingId);
            return Page.empty();
        }

        Page<Bid> bids = bidRepository.findByBiddingSessionId(session.getId(), pageable);
        log.debug("Found {} bids in DB for session: {}", bids.getNumberOfElements(), session.getId());
        return bids.map(b -> BidResponse.builder()
                .id(b.getId())
                .biddingSessionId(session.getId())
                .bidderId(b.getBidderId())
                .amount(b.getAmount())
                .timestamp(b.getTimestamp())
                .build());
    }

    @Transactional
    public BidResponse placeBid(Long listingId, BidRequest request) {
        log.debug("Entering placeBid for listingId: {}, amount: {}, bidderId: {}", listingId, request.getAmount(), request.getBidderId());
        // Find listing
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + listingId));

        BiddingSession session = sessionRepository.findByIdWithLock(listing.getBiddingSession().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Bidding session not found for listing: " + listingId));

        LocalDateTime now = LocalDateTime.now();

        // 1. Validation checks
        if (!session.getActive()) {
            throw new InvalidBidException("Bidding session is no longer active");
        }
        if (now.isBefore(session.getStartTime())) {
            throw new InvalidBidException("Bidding session has not started yet");
        }
        if (now.isAfter(session.getEndTime())) {
            throw new InvalidBidException("Bidding session has already expired");
        }
        if (listing.getSellerId().equals(request.getBidderId())) {
            throw new InvalidBidException("Sellers cannot bid on their own listings");
        }

        // Enforce KYC check for high-value categories
        if (isHighValueCategory(listing.getCategory())) {
            UserDto bidder = userClient.getUserById(request.getBidderId());
            if (bidder == null || !Boolean.TRUE.equals(bidder.getKycApproved())) {
                throw new InvalidBidException("Bidders must be KYC-Approved to place bids in high-value categories like real estate or vehicles.");
            }
        }

        // 2. Fetch current highest bid
        List<Bid> bids = bidRepository.findByBiddingSessionIdOrderByAmountDesc(session.getId());
        Bid currentHighestBid = bids.isEmpty() ? null : bids.get(0);

        if (currentHighestBid != null && currentHighestBid.getBidderId().equals(request.getBidderId())) {
            throw new InvalidBidException("You are already the highest bidder");
        }

        BigDecimal minRequiredBid;
        if (currentHighestBid == null) {
            minRequiredBid = session.getReservePrice();
        } else {
            minRequiredBid = currentHighestBid.getAmount().add(session.getBidIncrement());
        }

        if (request.getAmount().compareTo(minRequiredBid) < 0) {
            throw new InvalidBidException("Bid amount " + request.getAmount() + " must be at least " + minRequiredBid);
        }

        // 3. Escrow locking (10% of bid amount)
        BigDecimal requiredEscrow = request.getAmount().multiply(ESCROW_RATE);
        walletService.lockFunds(request.getBidderId(), requiredEscrow);

        // 4. Release previous highest bidder's escrow
        if (currentHighestBid != null) {
            BigDecimal previousEscrow = currentHighestBid.getAmount().multiply(ESCROW_RATE);
            walletService.releaseFunds(currentHighestBid.getBidderId(), previousEscrow);
            log.info("Released escrow of {} for previous bidder {}", previousEscrow, currentHighestBid.getBidderId());
        }

        // 5. Save the new bid
        Bid bid = Bid.builder()
                .biddingSession(session)
                .bidderId(request.getBidderId())
                .amount(request.getAmount())
                .timestamp(now)
                .build();
        bid = bidRepository.save(bid);

        // 6. Anti-sniping logic (Popcorn bidding): extend timer by 5 minutes if placed within the last 5 minutes
        long minutesLeft = ChronoUnit.MINUTES.between(now, session.getEndTime());
        if (minutesLeft < 5) {
            LocalDateTime originalEndTime = session.getEndTime();
            session.setEndTime(originalEndTime.plusMinutes(5));
            log.info("Anti-sniping triggered. Extended auction {} end time from {} to {}", session.getId(), originalEndTime, session.getEndTime());
        }

        // 7. Buy It Now logic
        if (session.getBuyItNowPrice() != null && request.getAmount().compareTo(session.getBuyItNowPrice()) >= 0) {
            session.setActive(false);
            session.setEndTime(now);
            log.info("Buy It Now price met for session {}. Bidding ended immediately.", session.getId());
        }

        sessionRepository.save(session);

        // Broadcast WebSocket update
        try {
            String wsMessage = objectMapper.writeValueAsString(Map.of(
                "type", "BID_PLACED",
                "listingId", listingId,
                "amount", request.getAmount(),
                "bidderId", request.getBidderId(),
                "endTime", session.getEndTime().toString(),
                "currentHighestBid", request.getAmount(),
                "active", session.getActive()
            ));
            webSocketHandler.broadcast(wsMessage);
        } catch (Exception e) {
            log.error("Failed to broadcast WebSocket message for bid: {}", e.getMessage());
        }

        // 8. Publish RabbitMQ events
        try {
            UserDto bidder = userClient.getUserById(request.getBidderId());
            if (bidder != null) {
                AuctionEventDto bidEvent = AuctionEventDto.builder()
                        .type("BID_PLACED")
                        .listingTitle(listing.getTitle())
                        .recipientEmail(bidder.getEmail())
                        .recipientName(bidder.getUsername())
                        .amount(request.getAmount())
                        .build();
                rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, "auction.event.bid", bidEvent);
            }

            if (currentHighestBid != null) {
                UserDto previousBidder = userClient.getUserById(currentHighestBid.getBidderId());
                if (previousBidder != null) {
                    AuctionEventDto outbidEvent = AuctionEventDto.builder()
                            .type("OUTBID")
                            .listingTitle(listing.getTitle())
                            .recipientEmail(previousBidder.getEmail())
                            .recipientName(previousBidder.getUsername())
                            .amount(request.getAmount())
                            .build();
                    rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, "auction.event.outbid", outbidEvent);
                }
            }
        } catch (Exception e) {
            log.error("Failed to publish RabbitMQ event for bid on session {}: {}", session.getId(), e.getMessage());
        }

        return BidResponse.builder()
                .id(bid.getId())
                .biddingSessionId(session.getId())
                .bidderId(bid.getBidderId())
                .amount(bid.getAmount())
                .timestamp(bid.getTimestamp())
                .build();
    }

    @Transactional
    public ListingResponse confirmSale(Long listingId, Long sellerId, boolean confirm) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + listingId));

        if (!listing.getSellerId().equals(sellerId)) {
            throw new IllegalArgumentException("Only the seller can confirm or reject this sale");
        }

        BiddingSession session = listing.getBiddingSession();
        List<Bid> bids = bidRepository.findByBiddingSessionIdOrderByAmountDesc(session.getId());
        if (bids.isEmpty()) {
            throw new IllegalStateException("No bids were placed on this listing");
        }
        Bid winnerBid = bids.get(0);

        if (confirm) {
            listing.setConfirmed(true);
            log.info("Seller {} confirmed sale of listing {} to winner {}", sellerId, listingId, winnerBid.getBidderId());
        } else {
            // Reject sale - refund winner's escrow deposit
            BigDecimal escrowAmount = winnerBid.getAmount().multiply(ESCROW_RATE);
            walletService.releaseFunds(winnerBid.getBidderId(), escrowAmount);
            session.setActive(false);
            sessionRepository.save(session);
            log.info("Seller {} rejected sale. Refunding winner {} deposit of {}", sellerId, winnerBid.getBidderId(), escrowAmount);
        }

        return mapToListingResponse(listingRepository.save(listing));
    }

    @Transactional
    public ListingResponse finalizeCheckout(Long listingId, Long winnerId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + listingId));

        if (!listing.getConfirmed()) {
            throw new IllegalStateException("Listing must be confirmed by the seller before checkout");
        }
        if (listing.getPaid()) {
            throw new IllegalStateException("Listing has already been paid");
        }

        BiddingSession session = listing.getBiddingSession();
        List<Bid> bids = bidRepository.findByBiddingSessionIdOrderByAmountDesc(session.getId());
        if (bids.isEmpty()) {
            throw new IllegalStateException("No bids were placed on this listing");
        }
        Bid winnerBid = bids.get(0);

        if (!winnerBid.getBidderId().equals(winnerId)) {
            throw new IllegalArgumentException("Only the winning bidder can finalize checkout");
        }

        BigDecimal totalAmount = winnerBid.getAmount();
        BigDecimal lockedEscrow = totalAmount.multiply(ESCROW_RATE);
        BigDecimal remainingBalance = totalAmount.subtract(lockedEscrow);

        // Deduct remaining balance from winner's available balance
        walletService.getOrCreateWalletWithLock(winnerId); // ensures lock
        walletService.lockFunds(winnerId, remainingBalance); // temporarily lock the remainder
        
        // Now charge the full amount: remainingBalance (which is locked now) + lockedEscrow (which was locked during bid)
        walletService.chargeFunds(winnerId, lockedEscrow);
        walletService.chargeFunds(winnerId, remainingBalance);

        // Transfer full bid amount to the seller's wallet balance
        walletService.depositFunds(listing.getSellerId(), totalAmount);

        listing.setPaid(true);
        session.setActive(false);
        sessionRepository.save(session);

        log.info("Finalized payment for listing {}. Winner {} paid {}, transferred to seller {}", listingId, winnerId, totalAmount, listing.getSellerId());

        return mapToListingResponse(listingRepository.save(listing));
    }

    @Transactional
    public void handleBuyerForfeiture(Long listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + listingId));

        if (!listing.getConfirmed()) {
            throw new IllegalStateException("Cannot forfeit a listing that was not confirmed");
        }
        if (listing.getPaid()) {
            throw new IllegalStateException("Cannot forfeit a paid listing");
        }

        BiddingSession session = listing.getBiddingSession();
        List<Bid> bids = bidRepository.findByBiddingSessionIdOrderByAmountDesc(session.getId());
        if (bids.isEmpty()) {
            return;
        }
        Bid winnerBid = bids.get(0);

        BigDecimal escrowAmount = winnerBid.getAmount().multiply(ESCROW_RATE);

        // Stripping escrow from the winner and transferring to the seller as compensation
        walletService.chargeFunds(winnerBid.getBidderId(), escrowAmount);
        walletService.depositFunds(listing.getSellerId(), escrowAmount);

        session.setActive(false);
        sessionRepository.save(session);

        log.warn("Buyer forfeiture triggered for listing {}. Transferred winning escrow of {} from winner {} to seller {} as penalty.", 
                listingId, escrowAmount, winnerBid.getBidderId(), listing.getSellerId());
    }

    @Transactional
    public void closeSession(BiddingSession session) {
        session.setActive(false);
        sessionRepository.save(session);
        log.info("Closed expired bidding session {} for listing {}", session.getId(), session.getListing().getTitle());

        // Broadcast WebSocket update
        try {
            String wsMessage = objectMapper.writeValueAsString(Map.of(
                "type", "AUCTION_ENDED",
                "listingId", session.getListing().getId(),
                "active", false
            ));
            webSocketHandler.broadcast(wsMessage);
        } catch (Exception e) {
            log.error("Failed to broadcast WebSocket message for ended session: {}", e.getMessage());
        }

        try {
            List<Bid> bids = bidRepository.findByBiddingSessionIdOrderByAmountDesc(session.getId());
            if (!bids.isEmpty()) {
                Bid winnerBid = bids.get(0);
                UserDto winner = userClient.getUserById(winnerBid.getBidderId());
                UserDto seller = userClient.getUserById(session.getListing().getSellerId());

                if (winner != null) {
                    AuctionEventDto winnerEvent = AuctionEventDto.builder()
                            .type("AUCTION_ENDED")
                            .listingTitle(session.getListing().getTitle())
                            .recipientEmail(winner.getEmail())
                            .recipientName(winner.getUsername())
                            .amount(winnerBid.getAmount())
                            .extraMessage("Congratulations! You won the auction. Please finalize checkout.")
                            .build();
                    rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, "auction.event.ended", winnerEvent);
                }

                if (seller != null) {
                    AuctionEventDto sellerEvent = AuctionEventDto.builder()
                            .type("AUCTION_ENDED")
                            .listingTitle(session.getListing().getTitle())
                            .recipientEmail(seller.getEmail())
                            .recipientName(seller.getUsername())
                            .amount(winnerBid.getAmount())
                            .extraMessage("Your auction has ended. Winner determined.")
                            .build();
                    rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, "auction.event.ended", sellerEvent);
                }
            }
        } catch (Exception e) {
            log.error("Failed to publish RabbitMQ events for expired session {}: {}", session.getId(), e.getMessage());
        }
    }

    private ListingResponse mapToListingResponse(Listing listing) {
        BiddingSessionResponse sessionResponse = null;
        if (listing.getBiddingSession() != null) {
            BiddingSession session = listing.getBiddingSession();
            List<Bid> bids = bidRepository.findByBiddingSessionIdOrderByAmountDesc(session.getId());
            BigDecimal highestBid = bids.isEmpty() ? null : bids.get(0).getAmount();
            Long highestBidderId = bids.isEmpty() ? null : bids.get(0).getBidderId();

            java.util.Map<Long, String> usernameCache = new java.util.HashMap<>();
            java.util.List<BidResponse> bidResponses = bids.stream().map(b -> {
                String username = usernameCache.computeIfAbsent(b.getBidderId(), bidderId -> {
                    try {
                        UserDto user = userClient.getUserById(bidderId);
                        return user != null ? user.getUsername() : "User " + bidderId;
                    } catch (Exception e) {
                        return "User " + bidderId;
                    }
                });
                return BidResponse.builder()
                        .id(b.getId())
                        .biddingSessionId(session.getId())
                        .bidderId(b.getBidderId())
                        .bidderUsername(username)
                        .amount(b.getAmount())
                        .timestamp(b.getTimestamp())
                        .build();
            }).toList();

            sessionResponse = BiddingSessionResponse.builder()
                    .id(session.getId())
                    .startTime(session.getStartTime())
                    .endTime(session.getEndTime())
                    .reservePrice(session.getReservePrice())
                    .buyItNowPrice(session.getBuyItNowPrice())
                    .bidIncrement(session.getBidIncrement())
                    .active(session.getActive())
                    .currentHighestBid(highestBid)
                    .currentHighestBidderId(highestBidderId)
                    .bids(bidResponses)
                    .build();
        }

        return ListingResponse.builder()
                .id(listing.getId())
                .title(listing.getTitle())
                .description(listing.getDescription())
                .imageUrl(listing.getImageUrl())
                .category(listing.getCategory())
                .sellerId(listing.getSellerId())
                .confirmed(listing.getConfirmed())
                .paid(listing.getPaid())
                .biddingSession(sessionResponse)
                .build();
    }

    @Transactional
    public ListingResponse updateListing(Long id, ListingRequest request) {
        log.info("Updating listing id={} with request: {}", id, request);
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + id));

        // Enforce KYC check if category is changed to a high-value one
        if (!listing.getCategory().equalsIgnoreCase(request.getCategory()) && isHighValueCategory(request.getCategory())) {
            UserDto seller = userClient.getUserById(listing.getSellerId());
            if (seller == null || !Boolean.TRUE.equals(seller.getKycApproved())) {
                throw new IllegalArgumentException("Sellers must be KYC-Approved to list in high-value categories.");
            }
        }

        BiddingSession session = listing.getBiddingSession();
        boolean hasBids = session.getBids() != null && !session.getBids().isEmpty();

        if (hasBids) {
            // Do not allow updating key bidding parameters if bids have already been placed
            if (request.getReservePrice().compareTo(session.getReservePrice()) != 0 ||
                request.getBidIncrement().compareTo(session.getBidIncrement()) != 0 ||
                !request.getStartTime().isEqual(session.getStartTime()) ||
                !request.getEndTime().isEqual(session.getEndTime()) ||
                (request.getBuyItNowPrice() != null && session.getBuyItNowPrice() != null && request.getBuyItNowPrice().compareTo(session.getBuyItNowPrice()) != 0) ||
                (request.getBuyItNowPrice() == null && session.getBuyItNowPrice() != null) ||
                (request.getBuyItNowPrice() != null && session.getBuyItNowPrice() == null)) {
                throw new IllegalArgumentException("Cannot modify active bidding parameters (reserve price, increment, times) once bids have been placed.");
            }
        } else {
            // Safe to update bidding parameters
            LocalDateTime now = LocalDateTime.now();
            if (request.getEndTime().isBefore(request.getStartTime())) {
                throw new IllegalArgumentException("End time must be after the start time.");
            }
            if (request.getEndTime().isAfter(request.getStartTime().plusMonths(6))) {
                throw new IllegalArgumentException("End time must be at most 6 months from the start time.");
            }
            session.setStartTime(request.getStartTime());
            session.setEndTime(request.getEndTime());
            session.setReservePrice(request.getReservePrice());
            session.setBuyItNowPrice(request.getBuyItNowPrice());
            session.setBidIncrement(request.getBidIncrement());
            sessionRepository.save(session);
        }

        listing.setTitle(request.getTitle());
        listing.setDescription(request.getDescription());
        listing.setImageUrl(request.getImageUrl());
        listing.setCategory(request.getCategory());
        listingRepository.save(listing);

        return mapToListingResponse(listing);
    }

    @Transactional
    public void deleteListing(Long id) {
        log.info("Deleting listing id={}", id);
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + id));

        BiddingSession session = listing.getBiddingSession();
        if (session != null && session.getBids() != null && !session.getBids().isEmpty()) {
            throw new IllegalArgumentException("Cannot delete listing because bids have already been placed on it.");
        }

        listingRepository.delete(listing);
        log.info("Successfully deleted listing id={}", id);
    }

    public BidResponse getBidById(Long listingId, Long bidId) {
        Bid b = bidRepository.findById(bidId)
                .orElseThrow(() -> new ResourceNotFoundException("Bid not found with id: " + bidId));
        if (!b.getBiddingSession().getListing().getId().equals(listingId)) {
            throw new IllegalArgumentException("Bid does not belong to the specified listing.");
        }
        // fetch username
        String username = "User " + b.getBidderId();
        try {
            UserDto bidder = userClient.getUserById(b.getBidderId());
            if (bidder != null) {
                username = bidder.getUsername();
            }
        } catch (Exception e) {
            // ignore
        }
        return BidResponse.builder()
                .id(b.getId())
                .biddingSessionId(b.getBiddingSession().getId())
                .bidderId(b.getBidderId())
                .bidderUsername(username)
                .amount(b.getAmount())
                .timestamp(b.getTimestamp())
                .build();
    }

    @Transactional
    public void deleteBid(Long listingId, Long bidId) {
        log.info("Deleting bid {} for listing {}", bidId, listingId);
        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new ResourceNotFoundException("Bid not found with id: " + bidId));
        BiddingSession session = bid.getBiddingSession();
        if (!session.getListing().getId().equals(listingId)) {
            throw new IllegalArgumentException("Bid does not belong to the specified listing.");
        }

        if (!Boolean.TRUE.equals(session.getActive())) {
            throw new IllegalArgumentException("Cannot delete bids on a closed/ended auction session.");
        }

        List<Bid> bids = bidRepository.findByBiddingSessionIdOrderByAmountDesc(session.getId());
        if (!bids.isEmpty() && bids.get(0).getId().equals(bidId)) {
            // If we delete the highest bid, release its escrow
            BigDecimal escrowToRelease = bid.getAmount().multiply(ESCROW_RATE);
            walletService.releaseFunds(bid.getBidderId(), escrowToRelease);

            // Re-lock the escrow for the next highest bidder if any exists
            if (bids.size() > 1) {
                Bid nextHighestBid = bids.get(1);
                BigDecimal newEscrow = nextHighestBid.getAmount().multiply(ESCROW_RATE);
                walletService.lockFunds(nextHighestBid.getBidderId(), newEscrow);
                log.info("Transferred highest bidder status to next highest bidder: {}", nextHighestBid.getBidderId());
            }
        }

        bidRepository.delete(bid);
        log.info("Deleted bid {} successfully", bidId);
    }

    public BiddingSessionResponse getSessionById(Long id) {
        BiddingSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bidding session not found with id: " + id));
        
        BigDecimal highestBid = session.getBids() == null || session.getBids().isEmpty()
                ? null
                : session.getBids().stream()
                        .map(Bid::getAmount)
                        .max(BigDecimal::compareTo)
                        .orElse(null);

        Long highestBidderId = null;
        if (highestBid != null) {
            highestBidderId = session.getBids().stream()
                    .filter(b -> b.getAmount().compareTo(highestBid) == 0)
                    .map(Bid::getBidderId)
                    .findFirst()
                    .orElse(null);
        }

        List<BidResponse> bidResponses = session.getBids() == null ? List.of() : session.getBids().stream().map(b -> {
            String username = "User " + b.getBidderId();
            try {
                UserDto bidder = userClient.getUserById(b.getBidderId());
                if (bidder != null) {
                    username = bidder.getUsername();
                }
            } catch (Exception e) {
                // ignore
            }
            return BidResponse.builder()
                    .id(b.getId())
                    .biddingSessionId(session.getId())
                    .bidderId(b.getBidderId())
                    .bidderUsername(username)
                    .amount(b.getAmount())
                    .timestamp(b.getTimestamp())
                    .build();
        }).toList();

        return BiddingSessionResponse.builder()
                .id(session.getId())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .reservePrice(session.getReservePrice())
                .buyItNowPrice(session.getBuyItNowPrice())
                .bidIncrement(session.getBidIncrement())
                .active(session.getActive())
                .currentHighestBid(highestBid)
                .currentHighestBidderId(highestBidderId)
                .bids(bidResponses)
                .build();
    }

    @Transactional
    public BiddingSessionResponse updateSession(Long id, BiddingSessionRequest request) {
        log.info("Updating bidding session id={}", id);
        BiddingSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bidding session not found with id: " + id));

        boolean hasBids = session.getBids() != null && !session.getBids().isEmpty();
        if (hasBids) {
            if (request.getReservePrice().compareTo(session.getReservePrice()) != 0 ||
                request.getBidIncrement().compareTo(session.getBidIncrement()) != 0 ||
                !request.getStartTime().isEqual(session.getStartTime()) ||
                !request.getEndTime().isEqual(session.getEndTime()) ||
                (request.getBuyItNowPrice() != null && session.getBuyItNowPrice() != null && request.getBuyItNowPrice().compareTo(session.getBuyItNowPrice()) != 0) ||
                (request.getBuyItNowPrice() == null && session.getBuyItNowPrice() != null) ||
                (request.getBuyItNowPrice() != null && session.getBuyItNowPrice() == null)) {
                throw new IllegalArgumentException("Cannot modify active bidding parameters once bids have been placed.");
            }
        } else {
            LocalDateTime now = LocalDateTime.now();
            if (request.getEndTime().isBefore(request.getStartTime())) {
                throw new IllegalArgumentException("End time must be after the start time.");
            }
            if (request.getEndTime().isAfter(request.getStartTime().plusMonths(6))) {
                throw new IllegalArgumentException("End time must be at most 6 months from the start time.");
            }
            session.setStartTime(request.getStartTime());
            session.setEndTime(request.getEndTime());
            session.setReservePrice(request.getReservePrice());
            session.setBuyItNowPrice(request.getBuyItNowPrice());
            session.setBidIncrement(request.getBidIncrement());
            sessionRepository.save(session);
        }
        return getSessionById(id);
    }

    @Transactional
    public void deleteSession(Long id) {
        log.info("Deleting bidding session id={}", id);
        BiddingSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bidding session not found with id: " + id));
        if (session.getBids() != null && !session.getBids().isEmpty()) {
            throw new IllegalArgumentException("Cannot delete bidding session because bids have already been placed on it.");
        }
        sessionRepository.delete(session);
    }

    @Transactional
    public void deactivateListingsBySeller(Long sellerId) {
        log.info("Deactivating or deleting listings for sellerId={}", sellerId);
        List<Listing> listings = listingRepository.findBySellerId(sellerId);
        for (Listing listing : listings) {
            BiddingSession session = listing.getBiddingSession();
            if (session != null) {
                if (session.getBids() == null || session.getBids().isEmpty()) {
                    listingRepository.delete(listing);
                } else {
                    session.setActive(false);
                    sessionRepository.save(session);
                }
            } else {
                listingRepository.delete(listing);
            }
        }
    }
}
