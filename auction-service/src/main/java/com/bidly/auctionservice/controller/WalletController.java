package com.bidly.auctionservice.controller;

import com.bidly.auctionservice.dto.WalletDepositRequest;
import com.bidly.auctionservice.dto.WalletResponse;
import com.bidly.auctionservice.dto.WalletTransactionResponse;
import com.bidly.auctionservice.entity.UserWallet;
import com.bidly.auctionservice.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auctions/wallets/{userId}")
@RequiredArgsConstructor
@Slf4j
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
    @PreAuthorize("hasAnyAuthority('USER')")
    public ResponseEntity<WalletResponse> getWallet(@PathVariable Long userId) {
        log.debug("GET getWallet requested for userId: {}", userId);
        UserWallet wallet = walletService.getOrCreateWallet(userId);
        log.info("Successfully fetched/created wallet for user: {}, balance: {}", userId, wallet.getBalance());
        return ResponseEntity.ok(WalletResponse.builder()
                .userId(wallet.getUserId())
                .balance(wallet.getBalance())
                .lockedBalance(wallet.getLockedBalance())
                .transactions(mapTransactions(wallet))
                .build());
    }

    @PostMapping("/deposit")
    @PreAuthorize("hasAnyAuthority('USER')")
    public ResponseEntity<WalletResponse> depositFunds(
            @PathVariable Long userId,
            @Valid @RequestBody WalletDepositRequest request
    ) {
        log.debug("POST depositFunds requested for userId: {}, amount: {}", userId, request.getAmount());
        UserWallet wallet = walletService.depositFunds(userId, request.getAmount());
        log.info("Successfully deposited {} to wallet of user {}", request.getAmount(), userId);
        return ResponseEntity.ok(WalletResponse.builder()
                .userId(wallet.getUserId())
                .balance(wallet.getBalance())
                .lockedBalance(wallet.getLockedBalance())
                .transactions(mapTransactions(wallet))
                .build());
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasAnyAuthority('USER')")
    public ResponseEntity<Page<WalletTransactionResponse>> getTransactions(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "timestamp,desc") String[] sort
    ) {
        log.debug("GET getTransactions requested for userId: {}, page: {}, size: {}, sort: {}", userId, page, size, sort);
        String sortField = sort[0];
        Sort.Direction sortDirection = Sort.Direction.fromString(sort.length > 1 ? sort[1] : "desc");
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortField));
        Page<WalletTransactionResponse> transactions = walletService.getTransactions(userId, pageable);
        log.info("Fetched page {} of transactions for user {}, found {} elements", page, userId, transactions.getNumberOfElements());
        return ResponseEntity.ok(transactions);
    }

    @PutMapping
    @PreAuthorize("hasAnyAuthority('USER')")
    public ResponseEntity<WalletResponse> updateWallet(
            @PathVariable Long userId,
            @RequestParam java.math.BigDecimal balance,
            @RequestParam java.math.BigDecimal lockedBalance
    ) {
        log.debug("PUT updateWallet requested for userId: {}, balance: {}, lockedBalance: {}", userId, balance, lockedBalance);
        UserWallet wallet = walletService.updateWallet(userId, balance, lockedBalance);
        log.info("Successfully updated wallet for user {} to balance={}, lockedBalance={}", userId, balance, lockedBalance);
        return ResponseEntity.ok(WalletResponse.builder()
                .userId(wallet.getUserId())
                .balance(wallet.getBalance())
                .lockedBalance(wallet.getLockedBalance())
                .transactions(mapTransactions(wallet))
                .build());
    }

    @DeleteMapping
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public ResponseEntity<Void> deleteWallet(@PathVariable Long userId) {
        log.debug("DELETE deleteWallet requested for userId: {}", userId);
        walletService.deleteWallet(userId);
        log.info("Successfully deleted wallet for user {}", userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/transactions/{transactionId}")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public ResponseEntity<WalletTransactionResponse> getTransaction(
            @PathVariable Long userId,
            @PathVariable Long transactionId
    ) {
        log.debug("GET getTransaction requested for userId: {}, transactionId: {}", userId, transactionId);
        return ResponseEntity.ok(walletService.getTransactionById(userId, transactionId));
    }
}
