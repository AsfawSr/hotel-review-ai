package com.asfaw.review_ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param webhookUrl    Slack-compatible incoming webhook (POST {"text": ...}); blank disables alerts
 * @param publicBaseUrl frontend URL used to link to the review, e.g. https://hotel-review-two.vercel.app
 */
@ConfigurationProperties(prefix = "app.alerts")
public record AlertProperties(
        @DefaultValue("") String webhookUrl,
        @DefaultValue("") String publicBaseUrl
) {

    public boolean enabled() {
        return !webhookUrl.isBlank();
    }
}
