package com.asfaw.review_ai.service;

import com.asfaw.review_ai.model.entity.ReplyRevision;
import com.asfaw.review_ai.model.entity.Review;
import com.asfaw.review_ai.model.enums.ReplyStatus;
import com.asfaw.review_ai.repository.ReplyRevisionRepository;
import com.asfaw.review_ai.repository.ReviewRepository;
import com.asfaw.review_ai.web.api.dto.ReplyResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * The AI manager response is the implicit DRAFT; each edit/approval/send appends an auditable revision.
 */
@Service
@RequiredArgsConstructor
public class ReplyService {

    private final ReviewRepository reviewRepository;
    private final ReplyRevisionRepository revisionRepository;

    @Transactional(readOnly = true)
    public ReplyResponse get(Long reviewId) {
        String aiDraft = aiDraft(reviewId);
        List<ReplyRevision> history = revisionRepository.findByReviewIdOrderByIdAsc(reviewId);
        if (history.isEmpty()) {
            return new ReplyResponse(ReplyStatus.DRAFT, aiDraft, "AI", null, List.of());
        }
        ReplyRevision current = history.getLast();
        return new ReplyResponse(current.getStatus(), current.getText(), current.getAuthor(), current.getCreatedAt(),
                history.stream().map(ReplyResponse.Revision::from).toList());
    }

    @Transactional
    public ReplyResponse edit(Long reviewId, String text, String author) {
        return transition(reviewId, ReplyStatus.EDITED, text.strip(), author);
    }

    @Transactional
    public ReplyResponse approve(Long reviewId, String author) {
        return transition(reviewId, ReplyStatus.APPROVED, null, author);
    }

    @Transactional
    public ReplyResponse markSent(Long reviewId, String author) {
        return transition(reviewId, ReplyStatus.SENT, null, author);
    }

    private ReplyResponse transition(Long reviewId, ReplyStatus target, String newText, String author) {
        ReplyResponse current = get(reviewId);
        if (!current.status().canTransitionTo(target)) {
            throw new ResponseStatusException(CONFLICT, "Cannot change a " + current.status() + " reply to " + target);
        }
        String text = newText != null ? newText : current.text();
        revisionRepository.save(new ReplyRevision(reviewId, target, text, author));
        return get(reviewId);
    }

    private String aiDraft(Long reviewId) {
        Review review = reviewRepository.findWithAnalysisById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Review not found"));
        if (review.getAnalysis() == null) {
            throw new ResponseStatusException(CONFLICT, "The review has not been analyzed yet");
        }
        return review.getAnalysis().getManagerResponse();
    }
}
