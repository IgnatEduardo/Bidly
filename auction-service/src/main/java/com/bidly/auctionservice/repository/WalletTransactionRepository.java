package com.bidly.auctionservice.repository;

import com.bidly.auctionservice.entity.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    List<WalletTransaction> findByUserWalletId(Long userWalletId);
    Page<WalletTransaction> findByUserWalletUserId(Long userId, Pageable pageable);
}
