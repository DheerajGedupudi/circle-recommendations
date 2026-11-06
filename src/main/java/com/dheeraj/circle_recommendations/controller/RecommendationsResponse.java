package com.dheeraj.circle_recommendations.controller;

import java.util.List;

/** Response body of GET /users/{userId}/recommendations. */
public record RecommendationsResponse(
    long userId,
    boolean askForInterests,
    List<RecommendedCircle> recommendations) {

    /** One ranked main pick, with the facts an app needs to render a Circle card. */
    public record RecommendedCircle(
        int rank,
        long circleId,
        String name,
        String topic,
        String matchType,
        String format,
        String joinPolicy,
        int memberCount,
        Integer maxMembers,
        int postsLast30Days,
        String reason,
        List<RelatedCircle> alsoOnThisTopic) {
    }

    /** Another eligible Circle on the same topic, so the member can choose (e.g. by format). */
    public record RelatedCircle(long circleId, String name, String format) {
    }
}
