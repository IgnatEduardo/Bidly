package com.bidly.auctionservice.controller;

import com.bidly.auctionservice.dto.BidRequest;
import com.bidly.auctionservice.dto.BidResponse;
import com.bidly.auctionservice.service.AuctionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BidControllerUnitTest {

    private BidController controller;

    @Mock
    private AuctionService auctionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new BidController(auctionService);
    }

    @Test
    void placeBid_returnsCreated() {
        BidRequest req = new BidRequest(2L, new BigDecimal("150.00"));
        BidResponse resp = BidResponse.builder().id(100L).amount(new BigDecimal("150.00")).bidderId(2L).build();
        when(auctionService.placeBid(eq(1L), any(BidRequest.class))).thenReturn(resp);

        ResponseEntity<BidResponse> res = controller.placeBid(1L, req);
        assertEquals(201, res.getStatusCode().value());
        assertEquals(100L, res.getBody().getId());
        assertEquals(new BigDecimal("150.00"), res.getBody().getAmount());
    }

    @Test
    void getBids_returnsOk() {
        BidResponse resp = BidResponse.builder().id(100L).build();
        Page<BidResponse> page = new PageImpl<>(List.of(resp));
        when(auctionService.getBidsForListing(eq(1L), any())).thenReturn(page);

        ResponseEntity<Page<BidResponse>> res = controller.getBids(1L, 0, 10, new String[]{"amount", "desc"});
        assertEquals(200, res.getStatusCode().value());
        assertEquals(1, res.getBody().getTotalElements());
    }

    @Test
    void getBid_returnsOk() {
        BidResponse resp = BidResponse.builder().id(100L).amount(new BigDecimal("150.00")).build();
        when(auctionService.getBidById(1L, 100L)).thenReturn(resp);

        ResponseEntity<BidResponse> res = controller.getBid(1L, 100L);
        assertEquals(200, res.getStatusCode().value());
        assertEquals(100L, res.getBody().getId());
    }

    @Test
    void deleteBid_returnsNoContent() {
        doNothing().when(auctionService).deleteBid(1L, 100L);

        ResponseEntity<Void> res = controller.deleteBid(1L, 100L);
        assertEquals(204, res.getStatusCode().value());
        verify(auctionService).deleteBid(1L, 100L);
    }
}
