package com.asfaw.review_ai.service;

public record PolicyChangedEvent(Long policyId, boolean deleted) {
}
