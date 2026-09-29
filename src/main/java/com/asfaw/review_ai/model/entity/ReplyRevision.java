package com.asfaw.review_ai.model.entity;

import com.asfaw.review_ai.model.enums.ReplyStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "review_reply_revisions")
@Getter
@Setter
@NoArgsConstructor
public class ReplyRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "review_id", nullable = false)
    private Long reviewId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReplyStatus status;

    @Column(name = "text", nullable = false, length = 4000)
    private String text;

    @Column(name = "author", nullable = false, length = 100)
    private String author;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ReplyRevision(Long reviewId, ReplyStatus status, String text, String author) {
        this.reviewId = reviewId;
        this.status = status;
        this.text = text;
        this.author = author;
    }

    @PrePersist
    public void onCreate() {
        this.createdAt = Instant.now();
    }
}
