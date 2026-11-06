package com.dheeraj.circle_recommendations.entity;

import jakarta.persistence.Converter;

public enum MembershipRole implements DbEnum {
    MEMBER("member"),
    LEADER("leader");

    private final String dbValue;

    MembershipRole(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }

    @Converter
    public static class JpaConverter extends DbEnumConverter<MembershipRole> {
        public JpaConverter() {
            super(MembershipRole.class);
        }
    }
}
