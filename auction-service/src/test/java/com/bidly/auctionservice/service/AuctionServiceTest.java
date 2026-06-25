package com.bidly.auctionservice.service;

import com.bidly.auctionservice.client.UserClient;
import com.bidly.auctionservice.config.AuctionWebSocketHandler;
import com.bidly.auctionservice.dto.*;
import com.bidly.auctionservice.entity.Bid;
import com.bidly.auctionservice.entity.BiddingSession;
import com.bidly.auctionservice.entity.Listing;
import com.bidly.auctionservice.exception.classes.InvalidBidException;
import com.bidly.auctionservice.exception.classes.ResourceNotFoundException;
import com.bidly.auctionservice.repository.BidRepository;
import com.bidly.auctionservice.repository.BiddingSessionRepository;
import com.bidly.auctionservice.repository.ListingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuctionServiceTest {

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private BiddingSessionRepository sessionRepository;

    @Mock
    private BidRepository bidRepository;

    @Mock
    private WalletService walletService;

    @Mock
    private UserClient userClient;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private AuctionWebSocketHandler webSocketHandler;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AuctionService auctionService;

    @Test
    void testCreateListing_WithSchedule_Success() {
        ListingRequest request = ListingRequest.builder()
                .title("Rolex Submariner")
                .description("Vintage watch in good condition")
                .category("Electronics")
                .sellerId(1L)
                .startTime(LocalDateTime.now().plusMinutes(5))
                .endTime(LocalDateTime.now().plusHours(2))
                .reservePrice(new BigDecimal("1000.00"))
                .bidIncrement(new BigDecimal("50.00"))
                .buyItNowPrice(new BigDecimal("2000.00"))
                .build();

        when(listingRepository.save(any(Listing.class))).thenAnswer(invocation -> {
            Listing l = invocation.getArgument(0);
            l.setId(1L);
            return l;
        });

        when(sessionRepository.save(any(BiddingSession.class))).thenAnswer(invocation -> {
            BiddingSession s = invocation.getArgument(0);
            s.setId(10L);
            return s;
        });

        when(bidRepository.findByBiddingSessionIdOrderByAmountDesc(anyLong())).thenReturn(new ArrayList<>());

        ListingResponse response = auctionService.createListing(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Rolex Submariner", response.getTitle());
        assertNotNull(response.getBiddingSession());
        assertTrue(response.getBiddingSession().getActive());
        assertEquals(new BigDecimal("1000.00"), response.getBiddingSession().getReservePrice());
    }

    @Test
    void testCreateListing_NoSchedule_Success() {
        ListingRequest request = ListingRequest.builder()
                .title("Unscheduled Watch")
                .description("Vintage watch")
                .category("Electronics")
                .sellerId(1L)
                .reservePrice(new BigDecimal("500.00"))
                .bidIncrement(new BigDecimal("10.00"))
                .build();

        when(listingRepository.save(any(Listing.class))).thenAnswer(invocation -> {
            Listing l = invocation.getArgument(0);
            l.setId(2L);
            return l;
        });

        when(sessionRepository.save(any(BiddingSession.class))).thenAnswer(invocation -> {
            BiddingSession s = invocation.getArgument(0);
            s.setId(11L);
            return s;
        });

        when(bidRepository.findByBiddingSessionIdOrderByAmountDesc(anyLong())).thenReturn(new ArrayList<>());

        ListingResponse response = auctionService.createListing(request);

        assertNotNull(response);
        assertEquals(2L, response.getId());
        assertNotNull(response.getBiddingSession());
        assertFalse(response.getBiddingSession().getActive());
        assertNull(response.getBiddingSession().getStartTime());
        assertNull(response.getBiddingSession().getEndTime());
    }

    @Test
    void testCreateListing_HighValueKycNotApproved_ThrowsException() {
        ListingRequest request = ListingRequest.builder()
                .title("Fancy House")
                .description("Big villa")
                .category("Real Estate")
                .sellerId(1L)
                .reservePrice(new BigDecimal("100000.00"))
                .bidIncrement(new BigDecimal("1000.00"))
                .build();

        UserDto sellerDto = UserDto.builder().id(1L).kycApproved(false).username("unapproved").build();
        when(userClient.getUserById(1L)).thenReturn(sellerDto);

        assertThrows(IllegalArgumentException.class, () -> auctionService.createListing(request));
    }

    @Test
    void testCreateListing_InvalidPrices_ThrowsException() {
        ListingRequest request = ListingRequest.builder()
                .title("Watch")
                .category("Others")
                .sellerId(1L)
                .reservePrice(BigDecimal.ZERO)
                .bidIncrement(new BigDecimal("10.00"))
                .build();

        assertThrows(IllegalArgumentException.class, () -> auctionService.createListing(request));
    }

    @Test
    void testGetListings() {
        Pageable pageable = PageRequest.of(0, 10);
        Listing listing = Listing.builder().id(1L).title("Test").category("Others").sellerId(1L).build();
        Page<Listing> page = new PageImpl<>(List.of(listing));

        when(listingRepository.findAll(pageable)).thenReturn(page);

        Page<ListingResponse> result = auctionService.getListings(pageable);
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Test", result.getContent().get(0).getTitle());
    }

    @Test
    void testGetListing_Success() {
        Listing listing = Listing.builder().id(5L).title("Found").category("Others").sellerId(1L).build();
        when(listingRepository.findById(5L)).thenReturn(Optional.of(listing));

        ListingResponse response = auctionService.getListing(5L);
        assertNotNull(response);
        assertEquals(5L, response.getId());
    }

    @Test
    void testGetListing_NotFound_ThrowsException() {
        when(listingRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> auctionService.getListing(99L));
    }

    @Test
    void testGetBidsForListing() {
        Listing listing = Listing.builder().id(1L).sellerId(2L).build();
        BiddingSession session = BiddingSession.builder().id(10L).listing(listing).build();
        listing.setBiddingSession(session);

        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing));

        Pageable pageable = PageRequest.of(0, 10);
        Bid bid = Bid.builder().id(100L).amount(new BigDecimal("100.00")).bidderId(3L).build();
        Page<Bid> page = new PageImpl<>(List.of(bid));

        when(bidRepository.findByBiddingSessionId(10L, pageable)).thenReturn(page);

        Page<BidResponse> result = auctionService.getBidsForListing(1L, pageable);
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(new BigDecimal("100.00"), result.getContent().get(0).getAmount());
    }

    @Test
    void testPlaceBid_Success() {
        Listing listing = Listing.builder().id(1L).sellerId(2L).category("Others").build();
        BiddingSession session = BiddingSession.builder()
                .id(10L)
                .listing(listing)
                .active(true)
                .startTime(LocalDateTime.now().minusHours(1))
                .endTime(LocalDateTime.now().plusHours(1))
                .reservePrice(new BigDecimal("100.00"))
                .bidIncrement(new BigDecimal("10.00"))
                .build();
        listing.setBiddingSession(session);

        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing));
        when(sessionRepository.findByIdWithLock(10L)).thenReturn(Optional.of(session));
        when(bidRepository.findByBiddingSessionIdOrderByAmountDesc(10L)).thenReturn(new ArrayList<>());
        when(bidRepository.save(any(Bid.class))).thenAnswer(inv -> inv.getArgument(0));

        BidRequest request = new BidRequest(3L, new BigDecimal("120.00"));

        BidResponse response = auctionService.placeBid(1L, request);
        assertNotNull(response);
        assertEquals(new BigDecimal("120.00"), response.getAmount());
        verify(walletService).lockFunds(eq(3L), eq(new BigDecimal("12.0000")));
    }

    @Test
    void testPlaceBid_InactiveSession_ThrowsException() {
        Listing listing = Listing.builder().id(1L).sellerId(2L).build();
        BiddingSession session = BiddingSession.builder()
                .id(10L)
                .listing(listing)
                .active(false)
                .build();
        listing.setBiddingSession(session);

        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing));
        when(sessionRepository.findByIdWithLock(10L)).thenReturn(Optional.of(session));

        BidRequest request = new BidRequest(3L, new BigDecimal("120.00"));
        assertThrows(InvalidBidException.class, () -> auctionService.placeBid(1L, request));
    }

    @Test
    void testPlaceBid_SelfBidding_ThrowsException() {
        Listing listing = Listing.builder().id(1L).sellerId(2L).category("Others").build();
        BiddingSession session = BiddingSession.builder()
                .id(10L)
                .listing(listing)
                .active(true)
                .startTime(LocalDateTime.now().minusHours(1))
                .endTime(LocalDateTime.now().plusHours(1))
                .build();
        listing.setBiddingSession(session);

        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing));
        when(sessionRepository.findByIdWithLock(10L)).thenReturn(Optional.of(session));

        BidRequest request = new BidRequest(2L, new BigDecimal("120.00")); // seller bidding
        assertThrows(InvalidBidException.class, () -> auctionService.placeBid(1L, request));
    }

    @Test
    void testConfirmSale_Confirm_Success() {
        Listing listing = Listing.builder().id(1L).sellerId(2L).confirmed(false).build();
        BiddingSession session = BiddingSession.builder().id(10L).listing(listing).build();
        listing.setBiddingSession(session);

        Bid winningBid = Bid.builder().id(50L).bidderId(3L).amount(new BigDecimal("500.00")).build();
        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing));
        when(bidRepository.findByBiddingSessionIdOrderByAmountDesc(10L)).thenReturn(List.of(winningBid));
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));

        ListingResponse response = auctionService.confirmSale(1L, 2L, true);
        assertNotNull(response);
        assertTrue(response.getConfirmed());
    }

    @Test
    void testConfirmSale_Reject_Success() {
        Listing listing = Listing.builder().id(1L).sellerId(2L).confirmed(false).build();
        BiddingSession session = BiddingSession.builder().id(10L).listing(listing).active(true).build();
        listing.setBiddingSession(session);

        Bid winningBid = Bid.builder().id(50L).bidderId(3L).amount(new BigDecimal("500.00")).build();
        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing));
        when(bidRepository.findByBiddingSessionIdOrderByAmountDesc(10L)).thenReturn(List.of(winningBid));
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));

        ListingResponse response = auctionService.confirmSale(1L, 2L, false);
        assertNotNull(response);
        assertFalse(response.getConfirmed());
        assertFalse(session.getActive());
        verify(walletService).releaseFunds(eq(3L), eq(new BigDecimal("50.0000")));
    }

    @Test
    void testFinalizeCheckout_Success() {
        Listing listing = Listing.builder().id(1L).sellerId(2L).confirmed(true).paid(false).build();
        BiddingSession session = BiddingSession.builder().id(10L).listing(listing).active(true).build();
        listing.setBiddingSession(session);

        Bid winningBid = Bid.builder().id(50L).bidderId(3L).amount(new BigDecimal("500.00")).build();
        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing));
        when(bidRepository.findByBiddingSessionIdOrderByAmountDesc(10L)).thenReturn(List.of(winningBid));
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));

        ListingResponse response = auctionService.finalizeCheckout(1L, 3L);
        assertNotNull(response);
        assertTrue(response.getPaid());
        verify(walletService).lockFunds(eq(3L), eq(new BigDecimal("450.0000")));
        verify(walletService).chargeFunds(eq(3L), eq(new BigDecimal("50.0000")));
        verify(walletService).chargeFunds(eq(3L), eq(new BigDecimal("450.0000")));
        verify(walletService).depositFunds(eq(2L), eq(new BigDecimal("500.00")));
    }

    @Test
    void testHandleBuyerForfeiture() {
        Listing listing = Listing.builder().id(1L).sellerId(2L).confirmed(true).paid(false).build();
        BiddingSession session = BiddingSession.builder().id(10L).listing(listing).active(true).build();
        listing.setBiddingSession(session);

        Bid winningBid = Bid.builder().id(50L).bidderId(3L).amount(new BigDecimal("500.00")).build();
        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing));
        when(bidRepository.findByBiddingSessionIdOrderByAmountDesc(10L)).thenReturn(List.of(winningBid));

        auctionService.handleBuyerForfeiture(1L);

        verify(walletService).chargeFunds(eq(3L), eq(new BigDecimal("50.0000")));
        verify(walletService).depositFunds(eq(2L), eq(new BigDecimal("50.0000")));
        assertFalse(session.getActive());
    }

    @Test
    void testCloseSession() {
        Listing listing = Listing.builder().id(1L).title("Watches").build();
        BiddingSession session = BiddingSession.builder().id(10L).listing(listing).active(true).build();

        auctionService.closeSession(session);

        assertFalse(session.getActive());
        verify(sessionRepository).save(session);
    }

    @Test
    void testUpdateListing_NoBids_Success() {
        Listing listing = Listing.builder().id(1L).title("Old Watch").description("Vintage").category("Electronics").sellerId(2L).build();
        BiddingSession session = BiddingSession.builder()
                .id(10L)
                .listing(listing)
                .reservePrice(new BigDecimal("100.00"))
                .bidIncrement(new BigDecimal("10.00"))
                .build();
        listing.setBiddingSession(session);

        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));

        ListingRequest request = ListingRequest.builder()
                .title("New Watch Title")
                .description("Vintage description")
                .category("Fashion")
                .sellerId(2L)
                .reservePrice(new BigDecimal("150.00"))
                .bidIncrement(new BigDecimal("15.00"))
                .build();

        ListingResponse response = auctionService.updateListing(1L, request);
        assertNotNull(response);
        assertEquals("New Watch Title", response.getTitle());
        assertEquals("Fashion", response.getCategory());
        assertEquals(new BigDecimal("150.00"), response.getBiddingSession().getReservePrice());
    }

    @Test
    void testUpdateListing_HasBids_ThrowsException() {
        Listing listing = Listing.builder().id(1L).title("Old Watch").category("Electronics").sellerId(2L).build();
        BiddingSession session = BiddingSession.builder()
                .id(10L)
                .listing(listing)
                .reservePrice(new BigDecimal("100.00"))
                .bidIncrement(new BigDecimal("10.00"))
                .build();
        listing.setBiddingSession(session);

        Bid activeBid = Bid.builder().id(1L).amount(new BigDecimal("120.00")).build();
        session.setBids(List.of(activeBid));

        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing));

        ListingRequest request = ListingRequest.builder()
                .title("New Title")
                .category("Electronics")
                .sellerId(2L)
                .reservePrice(new BigDecimal("150.00")) // modified parameter
                .bidIncrement(new BigDecimal("10.00"))
                .build();

        assertThrows(IllegalArgumentException.class, () -> auctionService.updateListing(1L, request));
    }

    @Test
    void testDeleteListing_NoBids_Success() {
        Listing listing = Listing.builder().id(1L).build();
        BiddingSession session = BiddingSession.builder().id(10L).listing(listing).build();
        listing.setBiddingSession(session);

        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing));

        auctionService.deleteListing(1L);

        verify(listingRepository).delete(listing);
    }

    @Test
    void testDeleteListing_HasBids_ThrowsException() {
        Listing listing = Listing.builder().id(1L).build();
        BiddingSession session = BiddingSession.builder().id(10L).listing(listing).build();
        listing.setBiddingSession(session);

        Bid activeBid = Bid.builder().id(1L).build();
        session.setBids(List.of(activeBid));

        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing));

        assertThrows(IllegalArgumentException.class, () -> auctionService.deleteListing(1L));
    }

    @Test
    void testScheduleAuction_Success() {
        Listing listing = Listing.builder().id(1L).sellerId(2L).build();
        BiddingSession session = BiddingSession.builder().id(10L).listing(listing).active(false).build();
        listing.setBiddingSession(session);

        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing));
        when(sessionRepository.save(any(BiddingSession.class))).thenAnswer(inv -> inv.getArgument(0));
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));

        AuctionScheduleRequest request = AuctionScheduleRequest.builder()
                .startTime(LocalDateTime.now().plusMinutes(5))
                .endTime(LocalDateTime.now().plusHours(2))
                .buyItNowPrice(new BigDecimal("1000.00"))
                .build();

        ListingResponse response = auctionService.scheduleAuction(1L, request);
        assertNotNull(response);
        assertNotNull(response.getBiddingSession());
        assertTrue(response.getBiddingSession().getActive());
    }
}
