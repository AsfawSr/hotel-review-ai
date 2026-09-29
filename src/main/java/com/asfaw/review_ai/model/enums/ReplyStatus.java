package com.asfaw.review_ai.model.enums;

import java.util.Set;

/**
 * DRAFT (AI suggestion) -> EDITED -> APPROVED -> SENT. Editing an approved reply reopens it; SENT is final.
 */
public enum ReplyStatus {
    DRAFT,
    EDITED,
    APPROVED,
    SENT;

    public boolean canEdit() {
        return this != SENT;
    }

    public boolean canTransitionTo(ReplyStatus target) {
        return switch (target) {
            case EDITED -> canEdit();
            case APPROVED -> Set.of(DRAFT, EDITED).contains(this);
            case SENT -> this == APPROVED;
            case DRAFT -> false;
        };
    }
}
