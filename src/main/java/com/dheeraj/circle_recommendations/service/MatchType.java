package com.dheeraj.circle_recommendations.service;

/**
 * Why a Circle matched, in ranking order: stated interests beat inferred ones,
 * which beat "popular" (everything else).
 */
public enum MatchType {
    /** The Circle's topic is one the member picked. */
    STATED_INTEREST("stated_interest"),
    /** The Circle's topic matches a Circle the member already belongs to. */
    INFERRED_INTEREST("inferred_interest"),
    /** No topic match: one of the most active Circles. */
    POPULAR("popular");

    private final String apiValue;

    MatchType(String apiValue) {
        this.apiValue = apiValue;
    }

    public String apiValue() {
        return apiValue;
    }
}
