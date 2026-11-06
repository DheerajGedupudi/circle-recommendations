package com.dheeraj.circle_recommendations.entity;

import jakarta.persistence.Converter;

public enum CircleFormat implements DbEnum {
    IN_PERSON("in_person"),
    VIRTUAL("virtual"),
    HYBRID("hybrid");

    private final String dbValue;

    CircleFormat(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }

    @Converter
    public static class JpaConverter extends DbEnumConverter<CircleFormat> {
        public JpaConverter() {
            super(CircleFormat.class);
        }
    }
}
