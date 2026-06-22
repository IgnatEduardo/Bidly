package com.bidly.auctionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingResponse {
    private Long id;
    private String title;
    private String description;
    private String imageUrl;
    private String category;
    private Long sellerId;
    private Boolean confirmed;
    private Boolean paid;
    private BiddingSessionResponse biddingSession;
}
