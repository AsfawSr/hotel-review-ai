package com.asfaw.review_ai.repository;

import com.asfaw.review_ai.model.entity.Review;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @EntityGraph(attributePaths = "analysis")
    List<Review> findAllByOrderBySubmittedAtDesc();

    @Query("select avg(r.rating) from Review r where r.rating is not null")
    Double findAverageRating();

    @EntityGraph(attributePaths = {"analysis", "analysis.topics"})
    Optional<Review> findWithAnalysisById(Long id);
}
