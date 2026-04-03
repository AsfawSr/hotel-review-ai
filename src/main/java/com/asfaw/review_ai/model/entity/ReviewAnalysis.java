package com.asfaw.review_ai.model.entity;

import com.asfaw.review_ai.model.enums.Sentiment;
import com.asfaw.review_ai.model.enums.Topic;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "review_analyses", indexes = {
        @Index(name = "idx_review_analyses_sentiment", columnList = "sentiment"),
        @Index(name = "idx_review_analyses_main_topic", columnList = "main_topic")
})
@Getter
@Setter
@NoArgsConstructor
public class ReviewAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "review_id", nullable = false, unique = true)
    private Review review;

    @Enumerated(EnumType.STRING)
    @Column(name = "sentiment", nullable = false, length = 20)
    private Sentiment sentiment;

    @Column(name = "sentiment_score", nullable = false)
    private Integer sentimentScore;

    @ElementCollection(fetch = FetchType.LAZY)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "review_topics", joinColumns = @JoinColumn(name = "review_analysis_id"))
    @Column(name = "topic", nullable = false, length = 40)
    private Set<Topic> topics = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "main_topic", nullable = false, length = 40)
    private Topic mainTopic;

    @Column(name = "manager_response", nullable = false, length = 4000)
    private String managerResponse;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}

