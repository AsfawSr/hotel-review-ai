package com.asfaw.review_ai.service;

import com.asfaw.review_ai.model.entity.HotelPolicyDocument;
import com.asfaw.review_ai.repository.HotelPolicyDocumentRepository;
import com.asfaw.review_ai.web.api.dto.PolicyRequest;
import com.asfaw.review_ai.web.api.dto.PolicyResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class PolicyService {

    private final HotelPolicyDocumentRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<PolicyResponse> list() {
        return repository.findAllByOrderByCategoryAscTitleAsc().stream().map(PolicyResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PolicyResponse get(Long id) {
        return PolicyResponse.from(find(id));
    }

    @Transactional
    public PolicyResponse create(PolicyRequest request) {
        HotelPolicyDocument policy = new HotelPolicyDocument();
        apply(policy, request);
        HotelPolicyDocument saved = repository.saveAndFlush(policy);
        eventPublisher.publishEvent(new PolicyChangedEvent(saved.getId(), false));
        return PolicyResponse.from(saved);
    }

    @Transactional
    public PolicyResponse update(Long id, PolicyRequest request) {
        HotelPolicyDocument policy = find(id);
        apply(policy, request);
        HotelPolicyDocument saved = repository.saveAndFlush(policy);
        eventPublisher.publishEvent(new PolicyChangedEvent(id, false));
        return PolicyResponse.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(find(id));
        eventPublisher.publishEvent(new PolicyChangedEvent(id, true));
    }

    private HotelPolicyDocument find(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Policy not found"));
    }

    private static void apply(HotelPolicyDocument policy, PolicyRequest request) {
        policy.setTitle(request.title().strip());
        policy.setCategory(request.category().strip());
        policy.setContent(request.content().strip());
        policy.setSource(request.source() == null || request.source().isBlank() ? null : request.source().strip());
        policy.setEffectiveDate(request.effectiveDate());
        policy.setActive(request.active() == null || request.active());
        policy.getTags().clear();
        if (request.tags() != null) {
            request.tags().stream().map(String::strip).forEach(policy.getTags()::add);
        }
    }
}
