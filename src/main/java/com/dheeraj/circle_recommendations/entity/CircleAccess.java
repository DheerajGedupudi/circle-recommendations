package com.dheeraj.circle_recommendations.entity;

import jakarta.persistence.Converter;

public enum CircleAccess implements DbEnum {
    PUBLIC("public"),
    UNLISTED("unlisted");

    private final String dbValue;

    CircleAccess(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }

    @Converter
    public static class JpaConverter extends DbEnumConverter<CircleAccess> {
        public JpaConverter() {
            super(CircleAccess.class);
        }
    }
}
