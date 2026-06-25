package com.bidly.auctionservice.repository;

import com.bidly.auctionservice.entity.BiddingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface BiddingSessionRepository extends JpaRepository<BiddingSession, Long> {
    List<BiddingSession> findByActive(Boolean active);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM BiddingSession s WHERE s.id = :id")
    Optional<BiddingSession> findByIdWithLock(@Param("id") Long id);
}
