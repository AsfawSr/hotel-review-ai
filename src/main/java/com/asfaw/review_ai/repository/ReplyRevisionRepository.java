package com.asfaw.review_ai.repository;

import com.asfaw.review_ai.model.entity.ReplyRevision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReplyRevisionRepository extends JpaRepository<ReplyRevision, Long> {

    List<ReplyRevision> findByReviewIdOrderByIdAsc(Long reviewId);

    Optional<ReplyRevision> findFirstByReviewIdOrderByIdDesc(Long reviewId);
}
