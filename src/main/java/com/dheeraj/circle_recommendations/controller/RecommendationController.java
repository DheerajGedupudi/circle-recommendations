package com.dheeraj.circle_recommendations.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dheeraj.circle_recommendations.controller.RecommendationsResponse.RecommendedCircle;
import com.dheeraj.circle_recommendations.controller.RecommendationsResponse.RelatedCircle;
import com.dheeraj.circle_recommendations.service.CircleCandidate;
import com.dheeraj.circle_recommendations.service.Recommendation;
import com.dheeraj.circle_recommendations.service.RecommendationResult;
import com.dheeraj.circle_recommendations.service.RecommendationService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
public class RecommendationController {

    private final RecommendationService service;

    public RecommendationController(RecommendationService service) {
        this.service = service;
    }

    /**
     * Ranked Circles the user should join, each with a member-facing reason.
     * Unknown user: 404. limit outside 1..20: 400. No eligible Circles: 200 with an empty list.
     */
    @GetMapping("/users/{userId}/recommendations")
    public RecommendationsResponse recommend(
            @PathVariable long userId,
            @RequestParam(defaultValue = "5") @Min(1) @Max(20) int limit) {
        RecommendationResult result = service.recommend(userId, limit);
        return new RecommendationsResponse(
            userId,
            result.askForInterests(),
            result.recommendations().stream().map(RecommendationController::toResponse).toList());
    }

    private static RecommendedCircle toResponse(Recommendation recommendation) {
        CircleCandidate circle = recommendation.circle();
        return new RecommendedCircle(
            recommendation.rank(),
            circle.id(),
            circle.name(),
            circle.topicLabel(),
            recommendation.matchType().apiValue(),
            circle.format().dbValue(),
            circle.joinPolicy().dbValue(),
            circle.memberCount(),
            circle.maxMembers(),
            circle.feedPosts30d(),
            recommendation.reason(),
            recommendation.alsoOnThisTopic().stream()
                .map(c -> new RelatedCircle(c.id(), c.name(), c.format().dbValue()))
                .toList());
    }
}
