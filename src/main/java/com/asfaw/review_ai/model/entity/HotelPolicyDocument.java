package com.asfaw.review_ai.model.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "hotel_policy_documents", indexes = {
        @Index(name = "idx_policy_category", columnList = "category"),
        @Index(name = "idx_policy_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
public class HotelPolicyDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "category", nullable = false, length = 80)
    private String category;

    @Column(name = "content", nullable = false, length = 8000)
    private String content;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "hotel_policy_tags", joinColumns = @JoinColumn(name = "policy_id"))
    @Column(name = "tag", nullable = false, length = 60)
    private Set<String> tags = new LinkedHashSet<>();

    @Column(name = "source", length = 200)
    private String source;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "active", nullable = false)
    private boolean active = true;

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

