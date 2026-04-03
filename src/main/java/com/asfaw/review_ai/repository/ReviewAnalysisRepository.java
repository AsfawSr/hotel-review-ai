package com.asfaw.review_ai.repository;

import com.asfaw.review_ai.model.entity.ReviewAnalysis;
import com.asfaw.review_ai.model.enums.Sentiment;
import com.asfaw.review_ai.model.enums.Topic;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReviewAnalysisRepository extends JpaRepository<ReviewAnalysis, Long> {

    @Query("select ra.mainTopic, count(ra) from ReviewAnalysis ra group by ra.mainTopic order by count(ra) desc")
    List<Object[]> findTopMainTopics(Pageable pageable);

    @Query("select ra.sentiment, count(ra) from ReviewAnalysis ra group by ra.sentiment")
    List<Object[]> countBySentiment();

    @Query("select ra.mainTopic, count(ra) from ReviewAnalysis ra group by ra.mainTopic")
    List<Object[]> countByMainTopic();
}

