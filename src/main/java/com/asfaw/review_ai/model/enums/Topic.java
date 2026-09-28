package com.asfaw.review_ai.model.enums;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum Topic {
    CLEANLINESS,
    STAFF,
    ACCESSIBILITY,
    FOOD,
    LOCATION,
    AMENITIES,
    VALUE,
    COMFORT,
    CHECK_IN,
    CHECK_OUT,
    NOISE,
    SAFETY,
    @JsonEnumDefaultValue
    OTHER
}

