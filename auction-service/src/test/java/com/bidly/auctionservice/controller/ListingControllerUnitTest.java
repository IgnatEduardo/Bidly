package com.bidly.auctionservice.controller;

import com.bidly.auctionservice.dto.*;
import com.bidly.auctionservice.service.AuctionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ListingControllerUnitTest {

    private ListingController controller;

    @Mock
    private AuctionService auctionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new ListingController(auctionService);
    }

    @Test
    void createListing_returnsCreated() {
        ListingRequest req = ListingRequest.builder().title("L").category("Cat").reservePrice(BigDecimal.TEN).bidIncrement(BigDecimal.ONE).sellerId(1L).build();
        ListingResponse resp = ListingResponse.builder().id(100L).title("L").build();
        when(auctionService.createListing(req)).thenReturn(resp);

        ResponseEntity<ListingResponse> res = controller.createListing(req);
        assertEquals(201, res.getStatusCode().value());
        assertEquals(100L, res.getBody().getId());
    }

    @Test
    void getListing_returnsOk() {
        ListingResponse resp = ListingResponse.builder().id(5L).title("Item").build();
        when(auctionService.getListing(5L)).thenReturn(resp);

        ResponseEntity<ListingResponse> res = controller.getListing(5L);
        assertEquals(200, res.getStatusCode().value());
        assertEquals("Item", res.getBody().getTitle());
    }

    @Test
    void getListings_returnsOk() {
        ListingResponse resp = ListingResponse.builder().id(5L).build();
        Page<ListingResponse> page = new PageImpl<>(List.of(resp));
        when(auctionService.getListings(any())).thenReturn(page);

        ResponseEntity<Page<ListingResponse>> res = controller.getListings(0, 10, new String[]{"id", "desc"});
        assertEquals(200, res.getStatusCode().value());
        assertEquals(1, res.getBody().getTotalElements());
    }

    @Test
    void confirmSale_returnsOk() {
        ListingResponse resp = ListingResponse.builder().id(1L).confirmed(true).build();
        when(auctionService.confirmSale(1L, 2L, true)).thenReturn(resp);

        ResponseEntity<ListingResponse> res = controller.confirmSale(1L, 2L, true);
        assertEquals(200, res.getStatusCode().value());
        assertTrue(res.getBody().getConfirmed());
    }

    @Test
    void finalizeCheckout_returnsOk() {
        ListingResponse resp = ListingResponse.builder().id(1L).paid(true).build();
        when(auctionService.finalizeCheckout(1L, 3L)).thenReturn(resp);

        ResponseEntity<ListingResponse> res = controller.finalizeCheckout(1L, 3L);
        assertEquals(200, res.getStatusCode().value());
        assertTrue(res.getBody().getPaid());
    }

    @Test
    void handleForfeiture_returnsOk() {
        doNothing().when(auctionService).handleBuyerForfeiture(1L);

        ResponseEntity<Void> res = controller.handleForfeiture(1L);
        assertEquals(200, res.getStatusCode().value());
        verify(auctionService).handleBuyerForfeiture(1L);
    }

    @Test
    void updateListing_returnsOk() {
        ListingRequest req = ListingRequest.builder().title("Updated").reservePrice(BigDecimal.TEN).bidIncrement(BigDecimal.ONE).sellerId(1L).build();
        ListingResponse resp = ListingResponse.builder().id(1L).title("Updated").build();
        when(auctionService.updateListing(1L, req)).thenReturn(resp);

        ResponseEntity<ListingResponse> res = controller.updateListing(1L, req);
        assertEquals(200, res.getStatusCode().value());
        assertEquals("Updated", res.getBody().getTitle());
    }

    @Test
    void deleteListing_returnsNoContent() {
        doNothing().when(auctionService).deleteListing(1L);

        ResponseEntity<Void> res = controller.deleteListing(1L);
        assertEquals(204, res.getStatusCode().value());
        verify(auctionService).deleteListing(1L);
    }

    @Test
    void deactivateListingsBySeller_returnsNoContent() {
        doNothing().when(auctionService).deactivateListingsBySeller(1L);

        ResponseEntity<Void> res = controller.deactivateListingsBySeller(1L);
        assertEquals(204, res.getStatusCode().value());
        verify(auctionService).deactivateListingsBySeller(1L);
    }

    @Test
    void scheduleAuction_returnsOk() {
        AuctionScheduleRequest req = AuctionScheduleRequest.builder().startTime(LocalDateTime.now()).endTime(LocalDateTime.now().plusHours(1)).build();
        ListingResponse resp = ListingResponse.builder().id(1L).build();
        when(auctionService.scheduleAuction(1L, req)).thenReturn(resp);

        ResponseEntity<ListingResponse> res = controller.scheduleAuction(1L, req);
        assertEquals(200, res.getStatusCode().value());
        verify(auctionService).scheduleAuction(1L, req);
    }
}
