package com.asfaw.review_ai.model.entity;

import com.asfaw.review_ai.model.enums.AnalysisStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import jakarta.persistence.FetchType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "reviews", indexes = {
        @Index(name = "idx_reviews_submitted_at", columnList = "submitted_at"),
        @Index(name = "idx_reviews_analysis_status", columnList = "analysis_status")
})
@Getter
@Setter
@NoArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(name = "guest_name", nullable = false, length = 100)
    private String guestName;

    @NotBlank
    @Size(max = 4000)
    @Column(name = "review_text", nullable = false, length = 4000)
    private String reviewText;

    @Min(1)
    @Max(5)
    @Column(name = "rating")
    private Integer rating;

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_status", length = 20)
    private AnalysisStatus analysisStatus;

    @Column(name = "analysis_error", length = 1000)
    private String analysisError;

    @Column(name = "analysis_updated_at")
    private Instant analysisUpdatedAt;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToOne(mappedBy = "review", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private ReviewAnalysis analysis;

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        this.submittedAt = now;
        this.updatedAt = now;
        if (this.analysisStatus == null) {
            this.analysisStatus = AnalysisStatus.PENDING;
        }
        this.analysisUpdatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
