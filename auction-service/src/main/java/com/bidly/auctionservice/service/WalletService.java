package com.bidly.auctionservice.service;

import com.bidly.auctionservice.entity.UserWallet;
import com.bidly.auctionservice.entity.WalletTransaction;
import com.bidly.auctionservice.exception.classes.InsufficientFundsException;
import com.bidly.auctionservice.exception.classes.ResourceNotFoundException;
import com.bidly.auctionservice.repository.UserWalletRepository;
import com.bidly.auctionservice.repository.WalletTransactionRepository;
import com.bidly.auctionservice.config.AuctionWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final UserWalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final AuctionWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    @Transactional
    public UserWallet getOrCreateWallet(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserWallet newWallet = UserWallet.builder()
                            .userId(userId)
                            .balance(BigDecimal.ZERO)
                            .lockedBalance(BigDecimal.ZERO)
                            .build();
                    return walletRepository.save(newWallet);
                });
    }

    @Transactional
    public UserWallet getOrCreateWalletWithLock(Long userId) {
        return walletRepository.findByUserIdWithLock(userId)
                .orElseGet(() -> {
                    UserWallet newWallet = UserWallet.builder()
                            .userId(userId)
                            .balance(BigDecimal.ZERO)
                            .lockedBalance(BigDecimal.ZERO)
                            .build();
                    return walletRepository.save(newWallet);
                });
    }

    @Transactional
    public UserWallet depositFunds(Long userId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than 0");
        }

        UserWallet wallet = getOrCreateWalletWithLock(userId);
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        WalletTransaction transaction = WalletTransaction.builder()
                .userWallet(wallet)
                .amount(amount)
                .type("DEPOSIT")
                .timestamp(LocalDateTime.now())
                .build();
        transactionRepository.save(transaction);

        // Broadcast WebSocket update
        try {
            String wsMessage = objectMapper.writeValueAsString(Map.of(
                "type", "WALLET_TRANSACTION",
                "userId", userId,
                "txType", "DEPOSIT",
                "amount", amount,
                "message", String.format("📥 Successfully deposited $%s to your wallet!", amount)
            ));
            webSocketHandler.broadcast(wsMessage);
        } catch (Exception e) {
            // ignore
        }

        return wallet;
    }

    @Transactional
    public void lockFunds(Long userId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        UserWallet wallet = getOrCreateWalletWithLock(userId);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds. Required deposit of " + amount + " but only had " + wallet.getBalance());
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        wallet.setLockedBalance(wallet.getLockedBalance().add(amount));
        walletRepository.save(wallet);

        WalletTransaction transaction = WalletTransaction.builder()
                .userWallet(wallet)
                .amount(amount)
                .type("LOCK")
                .timestamp(LocalDateTime.now())
                .build();
        transactionRepository.save(transaction);

        // Broadcast WebSocket update
        try {
            String wsMessage = objectMapper.writeValueAsString(Map.of(
                "type", "WALLET_TRANSACTION",
                "userId", userId,
                "txType", "LOCK",
                "amount", amount,
                "message", String.format("🔒 Escrow locked: $%s for active bid.", amount)
            ));
            webSocketHandler.broadcast(wsMessage);
        } catch (Exception e) {
            // ignore
        }
    }

    @Transactional
    public void releaseFunds(Long userId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        UserWallet wallet = getOrCreateWalletWithLock(userId);
        if (wallet.getLockedBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Cannot release " + amount + " since only " + wallet.getLockedBalance() + " is locked");
        }

        wallet.setLockedBalance(wallet.getLockedBalance().subtract(amount));
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        WalletTransaction transaction = WalletTransaction.builder()
                .userWallet(wallet)
                .amount(amount)
                .type("RELEASE")
                .timestamp(LocalDateTime.now())
                .build();
        transactionRepository.save(transaction);

        // Broadcast WebSocket update
        try {
            String wsMessage = objectMapper.writeValueAsString(Map.of(
                "type", "WALLET_TRANSACTION",
                "userId", userId,
                "txType", "RELEASE",
                "amount", amount,
                "message", String.format("🔓 Escrow released: $%s returned to your balance.", amount)
            ));
            webSocketHandler.broadcast(wsMessage);
        } catch (Exception e) {
            // ignore
        }
    }

    @Transactional
    public void chargeFunds(Long userId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        UserWallet wallet = getOrCreateWalletWithLock(userId);
        if (wallet.getLockedBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Cannot charge " + amount + " since only " + wallet.getLockedBalance() + " is locked");
        }

        wallet.setLockedBalance(wallet.getLockedBalance().subtract(amount));
        walletRepository.save(wallet);

        WalletTransaction transaction = WalletTransaction.builder()
                .userWallet(wallet)
                .amount(amount)
                .type("CHARGE")
                .timestamp(LocalDateTime.now())
                .build();
        transactionRepository.save(transaction);

        // Broadcast WebSocket update
        try {
            String wsMessage = objectMapper.writeValueAsString(Map.of(
                "type", "WALLET_TRANSACTION",
                "userId", userId,
                "txType", "CHARGE",
                "amount", amount,
                "message", String.format("💸 Checkout payment charged: $%s.", amount)
            ));
            webSocketHandler.broadcast(wsMessage);
        } catch (Exception e) {
            // ignore
        }
    }
}
