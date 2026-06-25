package com.bidly.auctionservice.controller;

import com.bidly.auctionservice.dto.BiddingSessionRequest;
import com.bidly.auctionservice.dto.BiddingSessionResponse;
import com.bidly.auctionservice.service.AuctionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BiddingSessionControllerUnitTest {

    private BiddingSessionController controller;

    @Mock
    private AuctionService auctionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new BiddingSessionController(auctionService);
    }

    @Test
    void getSession_returnsOk() {
        BiddingSessionResponse resp = BiddingSessionResponse.builder().id(10L).reservePrice(BigDecimal.TEN).build();
        when(auctionService.getSessionById(10L)).thenReturn(resp);

        ResponseEntity<BiddingSessionResponse> res = controller.getSession(10L);
        assertEquals(200, res.getStatusCode().value());
        assertEquals(10L, res.getBody().getId());
    }

    @Test
    void updateSession_returnsOk() {
        BiddingSessionRequest req = BiddingSessionRequest.builder().startTime(LocalDateTime.now()).endTime(LocalDateTime.now().plusHours(1)).reservePrice(BigDecimal.TEN).bidIncrement(BigDecimal.ONE).build();
        BiddingSessionResponse resp = BiddingSessionResponse.builder().id(10L).reservePrice(BigDecimal.TEN).build();
        when(auctionService.updateSession(10L, req)).thenReturn(resp);

        ResponseEntity<BiddingSessionResponse> res = controller.updateSession(10L, req);
        assertEquals(200, res.getStatusCode().value());
        assertEquals(10L, res.getBody().getId());
    }

    @Test
    void deleteSession_returnsNoContent() {
        doNothing().when(auctionService).deleteSession(10L);

        ResponseEntity<Void> res = controller.deleteSession(10L);
        assertEquals(204, res.getStatusCode().value());
        verify(auctionService).deleteSession(10L);
    }
}
