package com.bidly.auctionservice.controller;

import com.bidly.auctionservice.dto.WalletDepositRequest;
import com.bidly.auctionservice.dto.WalletResponse;
import com.bidly.auctionservice.dto.WalletTransactionResponse;
import com.bidly.auctionservice.entity.UserWallet;
import com.bidly.auctionservice.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auctions/wallets/{userId}")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    private List<WalletTransactionResponse> mapTransactions(UserWallet wallet) {
        if (wallet.getTransactions() == null) {
            return List.of();
        }
        return wallet.getTransactions().stream()
                .map(t -> WalletTransactionResponse.builder()
                        .id(t.getId())
                        .amount(t.getAmount())
                        .type(t.getType())
                        .timestamp(t.getTimestamp())
                        .build())
                .sorted((t1, t2) -> t2.getTimestamp().compareTo(t1.getTimestamp())) // Sort newest first
                .collect(Collectors.toList());
    }

    @GetMapping
    public ResponseEntity<WalletResponse> getWallet(@PathVariable Long userId) {
        UserWallet wallet = walletService.getOrCreateWallet(userId);
        return ResponseEntity.ok(WalletResponse.builder()
                .userId(wallet.getUserId())
                .balance(wallet.getBalance())
                .lockedBalance(wallet.getLockedBalance())
                .transactions(mapTransactions(wallet))
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
                .transactions(mapTransactions(wallet))
                .build());
    }
}
