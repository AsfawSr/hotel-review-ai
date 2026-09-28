package com.asfaw.review_ai.model.enums;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum Sentiment {
    POSITIVE,
    @JsonEnumDefaultValue
    NEUTRAL,
    NEGATIVE
}

