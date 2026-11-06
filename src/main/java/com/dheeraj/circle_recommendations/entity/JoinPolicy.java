package com.dheeraj.circle_recommendations.entity;

import jakarta.persistence.Converter;

public enum JoinPolicy implements DbEnum {
    OPEN("open"),
    REQUEST("request");

    private final String dbValue;

    JoinPolicy(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }

    @Converter
    public static class JpaConverter extends DbEnumConverter<JoinPolicy> {
        public JpaConverter() {
            super(JoinPolicy.class);
        }
    }
}
