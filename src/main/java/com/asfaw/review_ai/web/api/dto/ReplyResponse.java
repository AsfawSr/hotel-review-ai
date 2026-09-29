package com.asfaw.review_ai.web.api.dto;

import com.asfaw.review_ai.model.entity.ReplyRevision;
import com.asfaw.review_ai.model.enums.ReplyStatus;

import java.time.Instant;
import java.util.List;

public record ReplyResponse(
        ReplyStatus status,
        String text,
        String updatedBy,
        Instant updatedAt,
        List<Revision> history
) {

    public record Revision(ReplyStatus status, String text, String author, Instant createdAt) {

        public static Revision from(ReplyRevision revision) {
            return new Revision(revision.getStatus(), revision.getText(), revision.getAuthor(), revision.getCreatedAt());
        }
    }
}
