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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionService {

    private final ListingRepository listingRepository;
    private final BiddingSessionRepository sessionRepository;
    private final BidRepository bidRepository;
    private final WalletService walletService;

    private static final BigDecimal ESCROW_RATE = new BigDecimal("0.10"); // 10% deposit required

    @Transactional
    public ListingResponse createListing(ListingRequest request) {
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
        return listingRepository.findAll(pageable).map(this::mapToListingResponse);
    }

    public ListingResponse getListing(Long id) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + id));
        return mapToListingResponse(listing);
    }

    @Transactional
    public BidResponse placeBid(Long listingId, BidRequest request) {
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

        // 2. Fetch current highest bid
        List<Bid> bids = bidRepository.findByBiddingSessionIdOrderByAmountDesc(session.getId());
        Bid currentHighestBid = bids.isEmpty() ? null : bids.get(0);

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

    private ListingResponse mapToListingResponse(Listing listing) {
        BiddingSessionResponse sessionResponse = null;
        if (listing.getBiddingSession() != null) {
            BiddingSession session = listing.getBiddingSession();
            List<Bid> bids = bidRepository.findByBiddingSessionIdOrderByAmountDesc(session.getId());
            BigDecimal highestBid = bids.isEmpty() ? null : bids.get(0).getAmount();

            sessionResponse = BiddingSessionResponse.builder()
                    .id(session.getId())
                    .startTime(session.getStartTime())
                    .endTime(session.getEndTime())
                    .reservePrice(session.getReservePrice())
                    .buyItNowPrice(session.getBuyItNowPrice())
                    .bidIncrement(session.getBidIncrement())
                    .active(session.getActive())
                    .currentHighestBid(highestBid)
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
}
