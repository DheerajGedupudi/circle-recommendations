package com.dheeraj.circle_recommendations.service;

import java.util.List;

/**
 * One ranked main pick.
 *
 * @param alsoOnThisTopic lower-ranked eligible Circles on the same topic, best first
 */
public record Recommendation(
    int rank,
    CircleCandidate circle,
    MatchType matchType,
    String reason,
    List<CircleCandidate> alsoOnThisTopic) {
}
