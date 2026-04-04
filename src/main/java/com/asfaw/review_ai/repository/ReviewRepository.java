package com.asfaw.review_ai.repository;

import com.asfaw.review_ai.model.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

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
}
