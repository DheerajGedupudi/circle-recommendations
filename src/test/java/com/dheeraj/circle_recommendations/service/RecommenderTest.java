package com.dheeraj.circle_recommendations.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.dheeraj.circle_recommendations.entity.CircleAccess;
import com.dheeraj.circle_recommendations.entity.CircleFormat;
import com.dheeraj.circle_recommendations.entity.JoinPolicy;

/** Unit tests for the ranking rules. No Spring, no database. */
class RecommenderTest {

    private final Recommender recommender = new Recommender();

    @Test
    void removesJoinedFullInactiveAndUnlistedCircles() {
        List<CircleCandidate> circles = List.of(
            circle(1, 100L, 10),                                     // already a member
            circle(2, 101L, 10, JoinPolicy.OPEN, CircleAccess.PUBLIC, 12, 12),   // full
            circle(3, 102L, 0),                                      // inactive
            circle(4, 103L, 10, JoinPolicy.OPEN, CircleAccess.UNLISTED, null, 5), // unlisted
            circle(5, 104L, 10));                                    // eligible

        RecommendationResult result = recommender.recommend(Set.of(), Set.of(1L), circles, 10);

        assertThat(ids(result)).containsExactly(5L);
    }

    @Test
    void uncappedCirclesAreNeverFull() {
        List<CircleCandidate> circles = List.of(
            circle(1, 100L, 10, JoinPolicy.OPEN, CircleAccess.PUBLIC, null, 500));

        assertThat(ids(recommender.recommend(Set.of(), Set.of(), circles, 10))).containsExactly(1L);
    }

    @Test
    void ranksStatedThenInferredThenPopular() {
        List<CircleCandidate> circles = List.of(
            circle(10, 100L, 1),    // stated topic, barely active
            circle(20, 200L, 5),    // the member's own Circle (topic 200)
            circle(21, 200L, 5),    // inferred topic 200
            circle(30, 300L, 50));  // no match, very active

        RecommendationResult result = recommender.recommend(Set.of(100L), Set.of(20L), circles, 10);

        assertThat(ids(result)).containsExactly(10L, 21L, 30L);
        assertThat(result.recommendations()).extracting(Recommendation::matchType).containsExactly(
            MatchType.STATED_INTEREST, MatchType.INFERRED_INTEREST, MatchType.POPULAR);
    }

    @Test
    void mostPostsFirstAndTiesGoToLowerId() {
        List<CircleCandidate> circles = List.of(
            circle(7, 100L, 5),
            circle(5, 101L, 9),
            circle(3, 102L, 9));

        assertThat(ids(recommender.recommend(Set.of(), Set.of(), circles, 10))).containsExactly(3L, 5L, 7L);
    }

    @Test
    void sameTopicCirclesAreGroupedUnderTheMostActiveOne() {
        List<CircleCandidate> circles = List.of(
            circle(1, 100L, 5),
            circle(2, 100L, 9),
            circle(3, 100L, 7));

        RecommendationResult result = recommender.recommend(Set.of(100L), Set.of(), circles, 10);

        assertThat(ids(result)).containsExactly(2L);
        assertThat(result.recommendations().get(0).alsoOnThisTopic())
            .extracting(CircleCandidate::id).containsExactly(3L, 1L);
    }

    @Test
    void circlesWithoutATopicAreAlwaysTheirOwnPick() {
        List<CircleCandidate> circles = List.of(
            circle(1, null, 9),
            circle(2, null, 8));

        assertThat(ids(recommender.recommend(Set.of(), Set.of(), circles, 10))).containsExactly(1L, 2L);
    }

    @Test
    void respectsTheLimit() {
        List<CircleCandidate> circles = List.of(circle(1, 100L, 3), circle(2, 101L, 2), circle(3, 102L, 1));

        RecommendationResult result = recommender.recommend(Set.of(), Set.of(), circles, 2);

        assertThat(ids(result)).containsExactly(1L, 2L);
        assertThat(result.recommendations()).extracting(Recommendation::rank).containsExactly(1, 2);
    }

    @Test
    void asksForInterestsOnlyWhenNoneAreStated() {
        List<CircleCandidate> circles = List.of(circle(1, 100L, 3));

        assertThat(recommender.recommend(Set.of(), Set.of(), circles, 5).askForInterests()).isTrue();
        assertThat(recommender.recommend(Set.of(100L), Set.of(), circles, 5).askForInterests()).isFalse();
    }

    @Test
    void reasonsExplainTheMatchAndShowActivity() {
        CircleCandidate open = circle(1, 100L, 14);
        CircleCandidate request = circle(2, 100L, 1, JoinPolicy.REQUEST, CircleAccess.PUBLIC, null, 5);
        CircleCandidate joined = new CircleCandidate(
            9, "Board-Ready Women", 100L, "Topic 100", CircleFormat.VIRTUAL, CircleAccess.PUBLIC,
            JoinPolicy.OPEN, null, 5, 3);

        assertThat(Recommender.reason(open, MatchType.STATED_INTEREST, null))
            .isEqualTo("Matches your interest in Topic 100. 14 posts in the last 30 days.");
        assertThat(Recommender.reason(open, MatchType.INFERRED_INTEREST, joined))
            .isEqualTo("You're in Board-Ready Women, so you may like another Topic 100 Circle."
                + " 14 posts in the last 30 days.");
        assertThat(Recommender.reason(open, MatchType.POPULAR, null))
            .isEqualTo("One of the most active Circles right now: 14 posts in the last 30 days.");
        assertThat(Recommender.reason(request, MatchType.STATED_INTEREST, null))
            .isEqualTo("Matches your interest in Topic 100. 1 post in the last 30 days."
                + " The leader approves new members.");
    }

    // ---- helpers ------------------------------------------------------------------------------

    private static CircleCandidate circle(long id, Long topicId, int posts) {
        return circle(id, topicId, posts, JoinPolicy.OPEN, CircleAccess.PUBLIC, null, 10);
    }

    private static CircleCandidate circle(
            long id, Long topicId, int posts, JoinPolicy joinPolicy, CircleAccess access,
            Integer maxMembers, int memberCount) {
        return new CircleCandidate(
            id, "Circle " + id, topicId, topicId == null ? null : "Topic " + topicId,
            CircleFormat.VIRTUAL, access, joinPolicy, maxMembers, memberCount, posts);
    }

    private static List<Long> ids(RecommendationResult result) {
        return result.recommendations().stream().map(r -> r.circle().id()).toList();
    }
}
