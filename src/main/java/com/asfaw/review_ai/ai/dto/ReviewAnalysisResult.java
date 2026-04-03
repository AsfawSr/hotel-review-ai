package com.asfaw.review_ai.ai.dto;

import com.asfaw.review_ai.model.enums.Sentiment;
import com.asfaw.review_ai.model.enums.Topic;

import java.util.Set;

public record ReviewAnalysisResult(
        Sentiment sentiment,
        Integer sentimentScore,
        Set<Topic> topics,
        Topic mainTopic,
        String managerResponse
) {
}

