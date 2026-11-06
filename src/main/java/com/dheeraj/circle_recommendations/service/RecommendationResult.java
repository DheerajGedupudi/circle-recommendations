package com.dheeraj.circle_recommendations.service;

import java.util.List;

/**
 * @param askForInterests true when the member has no stated interests, so the app can
 *                        prompt them to pick some (the real fix for cold start)
 */
public record RecommendationResult(boolean askForInterests, List<Recommendation> recommendations) {
}
