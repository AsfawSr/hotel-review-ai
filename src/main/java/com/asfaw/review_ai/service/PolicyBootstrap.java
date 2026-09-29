package com.asfaw.review_ai.service;

import com.asfaw.review_ai.ai.service.PolicyIngestionService;
import com.asfaw.review_ai.config.RagProperties;
import com.asfaw.review_ai.repository.HotelPolicyDocumentRepository;
import com.asfaw.review_ai.web.api.dto.PolicyRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Seeds sample hotel policies into an empty database and optionally (re)embeds all policies on startup.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PolicyBootstrap implements ApplicationRunner {

    private final HotelPolicyDocumentRepository repository;
    private final PolicyService policyService;
    private final PolicyIngestionService ingestionService;
    private final RagProperties ragProperties;
    private final ObjectMapper objectMapper;

    @Value("classpath:policies/sample-policies.json")
    private Resource samplePolicies;

    @Override
    public void run(ApplicationArguments args) {
        if (ragProperties.seedSamplePolicies() && repository.count() == 0) {
            List<PolicyRequest> policies = readSamplePolicies();
            policies.forEach(policyService::create);
            log.info("Seeded {} sample hotel policies", policies.size());
        }
        if (ragProperties.reindexOnStartup()) {
            try {
                ingestionService.reindexAll();
            } catch (RuntimeException ex) {
                // Usually the embedding model is not pulled yet; POST /api/v1/policies/reindex retries later.
                log.warn("Policy re-indexing on startup failed", ex);
            }
        }
    }

    private List<PolicyRequest> readSamplePolicies() {
        try (InputStream in = samplePolicies.getInputStream()) {
            return objectMapper.readValue(in, new TypeReference<>() {
            });
        } catch (IOException ex) {
            throw new IllegalStateException("Could not read sample policies", ex);
        }
    }
}
