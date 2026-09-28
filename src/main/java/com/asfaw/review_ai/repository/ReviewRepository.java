package com.asfaw.review_ai.repository;

import com.asfaw.review_ai.model.entity.Review;
import com.asfaw.review_ai.model.enums.AnalysisStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

    @EntityGraph(attributePaths = "analysis")
    List<Review> findAllByOrderBySubmittedAtDesc();

    @EntityGraph(attributePaths = "analysis")
    Page<Review> findAllByOrderBySubmittedAtDesc(Pageable pageable);

    @Query("select avg(r.rating) from Review r where r.rating is not null")
    Double findAverageRating();

    @Query("select r.rating, count(r) from Review r where r.rating is not null group by r.rating order by r.rating asc")
    List<Object[]> countByRating();

    @Query("select r.rating, count(r) from Review r where r.rating is not null group by r.rating order by count(r) desc, r.rating desc")
    List<Object[]> findTopRatings(Pageable pageable);

    @EntityGraph(attributePaths = {"analysis", "analysis.topics"})
    Optional<Review> findWithAnalysisById(Long id);

    /**
     * Atomically moves a review from {@code expected} to {@code target}; returns 0 if another worker got there first.
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Review r
               set r.analysisStatus = :target, r.analysisError = null, r.analysisUpdatedAt = :now, r.updatedAt = :now
             where r.id = :id and r.analysisStatus = :expected
            """)
    int transitionStatus(@Param("id") Long id,
                         @Param("expected") AnalysisStatus expected,
                         @Param("target") AnalysisStatus target,
                         @Param("now") Instant now);
}
