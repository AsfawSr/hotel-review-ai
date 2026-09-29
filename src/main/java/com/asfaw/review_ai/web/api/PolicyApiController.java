package com.asfaw.review_ai.web.api;

import com.asfaw.review_ai.ai.service.PolicyIngestionService;
import com.asfaw.review_ai.service.PolicyService;
import com.asfaw.review_ai.web.api.dto.PolicyRequest;
import com.asfaw.review_ai.web.api.dto.PolicyResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
public class PolicyApiController {

    private final PolicyService policyService;
    private final PolicyIngestionService ingestionService;

    @GetMapping
    public List<PolicyResponse> list() {
        return policyService.list();
    }

    @GetMapping("/{id}")
    public PolicyResponse get(@PathVariable Long id) {
        return policyService.get(id);
    }

    @PostMapping
    public ResponseEntity<PolicyResponse> create(@Valid @RequestBody PolicyRequest request) {
        PolicyResponse created = policyService.create(request);
        return ResponseEntity
                .created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id()).toUri())
                .body(created);
    }

    @PutMapping("/{id}")
    public PolicyResponse update(@PathVariable Long id, @Valid @RequestBody PolicyRequest request) {
        return policyService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        policyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reindex")
    public ReindexResponse reindex() {
        return new ReindexResponse(ingestionService.isAvailable(), ingestionService.reindexAll());
    }

    public record ReindexResponse(boolean ragEnabled, int indexed) {
    }
}
