package com.bidly.auctionservice.scheduler;

import com.bidly.auctionservice.service.AuctionService;
import com.bidly.auctionservice.repository.BiddingSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.bidly.auctionservice.entity.BiddingSession;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BiddingSessionScheduler {

    private final BiddingSessionRepository sessionRepository;
    private final AuctionService auctionService;

    @Scheduled(fixedRate = 10000) // check every 10 seconds
    @Transactional
    public void checkExpiredBiddingSessions() {
        LocalDateTime now = LocalDateTime.now();
        List<BiddingSession> activeSessions = sessionRepository.findByActive(true);

        for (BiddingSession session : activeSessions) {
            if (now.isAfter(session.getEndTime())) {
                auctionService.closeSession(session);
            }
        }
    }
}
