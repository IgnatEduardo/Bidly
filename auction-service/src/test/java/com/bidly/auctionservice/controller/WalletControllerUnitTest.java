package com.bidly.auctionservice.controller;

import com.bidly.auctionservice.dto.WalletDepositRequest;
import com.bidly.auctionservice.dto.WalletResponse;
import com.bidly.auctionservice.dto.WalletTransactionResponse;
import com.bidly.auctionservice.entity.UserWallet;
import com.bidly.auctionservice.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WalletControllerUnitTest {

    private WalletController controller;

    @Mock
    private WalletService walletService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new WalletController(walletService);
    }

    @Test
    void getWallet_returnsOk() {
        UserWallet wallet = UserWallet.builder().userId(1L).balance(BigDecimal.TEN).lockedBalance(BigDecimal.ZERO).transactions(new ArrayList<>()).build();
        when(walletService.getOrCreateWallet(1L)).thenReturn(wallet);

        ResponseEntity<WalletResponse> res = controller.getWallet(1L);
        assertEquals(200, res.getStatusCode().value());
        assertEquals(1L, res.getBody().getUserId());
        assertEquals(BigDecimal.TEN, res.getBody().getBalance());
    }

    @Test
    void depositFunds_returnsOk() {
        WalletDepositRequest req = new WalletDepositRequest(BigDecimal.TEN);
        UserWallet wallet = UserWallet.builder().userId(1L).balance(BigDecimal.TEN).lockedBalance(BigDecimal.ZERO).transactions(new ArrayList<>()).build();
        when(walletService.depositFunds(1L, BigDecimal.TEN)).thenReturn(wallet);

        ResponseEntity<WalletResponse> res = controller.depositFunds(1L, req);
        assertEquals(200, res.getStatusCode().value());
        assertEquals(BigDecimal.TEN, res.getBody().getBalance());
    }

    @Test
    void getTransactions_returnsOk() {
        WalletTransactionResponse tx = WalletTransactionResponse.builder().id(100L).amount(BigDecimal.TEN).build();
        Page<WalletTransactionResponse> page = new PageImpl<>(List.of(tx));
        when(walletService.getTransactions(eq(1L), any())).thenReturn(page);

        ResponseEntity<Page<WalletTransactionResponse>> res = controller.getTransactions(1L, 0, 10, new String[]{"timestamp", "desc"});
        assertEquals(200, res.getStatusCode().value());
        assertEquals(1, res.getBody().getTotalElements());
    }

    @Test
    void updateWallet_returnsOk() {
        UserWallet wallet = UserWallet.builder().userId(1L).balance(BigDecimal.TEN).lockedBalance(BigDecimal.ONE).transactions(new ArrayList<>()).build();
        when(walletService.updateWallet(1L, BigDecimal.TEN, BigDecimal.ONE)).thenReturn(wallet);

        ResponseEntity<WalletResponse> res = controller.updateWallet(1L, BigDecimal.TEN, BigDecimal.ONE);
        assertEquals(200, res.getStatusCode().value());
        assertEquals(BigDecimal.TEN, res.getBody().getBalance());
        assertEquals(BigDecimal.ONE, res.getBody().getLockedBalance());
    }

    @Test
    void deleteWallet_returnsNoContent() {
        doNothing().when(walletService).deleteWallet(1L);

        ResponseEntity<Void> res = controller.deleteWallet(1L);
        assertEquals(204, res.getStatusCode().value());
        verify(walletService).deleteWallet(1L);
    }

    @Test
    void getTransaction_returnsOk() {
        WalletTransactionResponse tx = WalletTransactionResponse.builder().id(100L).amount(BigDecimal.TEN).build();
        when(walletService.getTransactionById(1L, 100L)).thenReturn(tx);

        ResponseEntity<WalletTransactionResponse> res = controller.getTransaction(1L, 100L);
        assertEquals(200, res.getStatusCode().value());
        assertEquals(100L, res.getBody().getId());
    }
}
