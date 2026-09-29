package com.asfaw.review_ai.model.enums;

/**
 * VIEWER: read-only. MANAGER: submit/retry reviews and handle replies. ADMIN: everything incl. policies and users.
 */
public enum UserRole {
    ADMIN,
    MANAGER,
    VIEWER
}
