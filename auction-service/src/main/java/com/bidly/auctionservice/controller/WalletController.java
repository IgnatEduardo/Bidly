package com.bidly.auctionservice.controller;

import com.bidly.auctionservice.dto.WalletDepositRequest;
import com.bidly.auctionservice.dto.WalletResponse;
import com.bidly.auctionservice.entity.UserWallet;
import com.bidly.auctionservice.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auctions/wallets/{userId}")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    public ResponseEntity<WalletResponse> getWallet(@PathVariable Long userId) {
        UserWallet wallet = walletService.getOrCreateWallet(userId);
        return ResponseEntity.ok(WalletResponse.builder()
                .userId(wallet.getUserId())
                .balance(wallet.getBalance())
                .lockedBalance(wallet.getLockedBalance())
                .build());
    }

    @PostMapping("/deposit")
    public ResponseEntity<WalletResponse> depositFunds(
            @PathVariable Long userId,
            @Valid @RequestBody WalletDepositRequest request
    ) {
        UserWallet wallet = walletService.depositFunds(userId, request.getAmount());
        return ResponseEntity.ok(WalletResponse.builder()
                .userId(wallet.getUserId())
                .balance(wallet.getBalance())
                .lockedBalance(wallet.getLockedBalance())
                .build());
    }
}
