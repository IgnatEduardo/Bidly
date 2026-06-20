package com.bidly.auctionservice.scheduler;

import com.bidly.auctionservice.entity.BiddingSession;
import com.bidly.auctionservice.repository.BiddingSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BiddingSessionScheduler {

    private final BiddingSessionRepository sessionRepository;

    @Scheduled(fixedRate = 10000) // check every 10 seconds
    @Transactional
    public void checkExpiredBiddingSessions() {
        LocalDateTime now = LocalDateTime.now();
        List<BiddingSession> activeSessions = sessionRepository.findByActive(true);

        for (BiddingSession session : activeSessions) {
            if (now.isAfter(session.getEndTime())) {
                session.setActive(false);
                sessionRepository.save(session);
                log.info("Bidding session {} (Listing: {}) expired and has been closed automatically at {}", 
                        session.getId(), session.getListing().getTitle(), now);
            }
        }
    }
}
