package com.asfaw.review_ai.web.dto;

import com.asfaw.review_ai.model.enums.Sentiment;
import com.asfaw.review_ai.model.enums.Topic;

import java.time.Instant;

public record ReviewListItem(
        Long id,
        String guestName,
        Integer rating,
        Sentiment sentiment,
        Topic mainTopic,
        Instant submittedAt
) {
}

