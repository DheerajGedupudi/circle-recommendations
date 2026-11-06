package com.dheeraj.circle_recommendations.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dheeraj.circle_recommendations.entity.JoinPolicy;

/**
 * Ranks Circles for one member. Pure logic (no Spring, no database), so every rule is unit tested.
 *
 * <ol>
 *   <li><b>Remove</b> Circles that would be a dead end: already a member, full, inactive
 *       (no feed posts in 30 days), or unlisted. Request-to-join Circles stay: that's one
 *       extra step, not a dead end.</li>
 *   <li><b>Sort</b> into groups: stated interest, then inferred interest (topics of Circles the
 *       member already belongs to), then everything else. Within a group, most feed posts in
 *       the last 30 days first; ties go to the lower Circle id so the order is stable.</li>
 *   <li><b>Group duplicates</b>: the first Circle per topic is the main pick; later Circles on
 *       the same topic are attached to it as "also on this topic", because they can differ in
 *       format and the member should be able to choose. Circles without a topic are always
 *       their own pick.</li>
 *   <li><b>Explain</b> each main pick with a member-facing reason, and return the top {@code limit}.</li>
 * </ol>
 */
public final class Recommender {

    public RecommendationResult recommend(
            Set<Long> statedTopicIds,
            Set<Long> joinedCircleIds,
            List<CircleCandidate> allCircles,
            int limit) {

        Map<Long, CircleCandidate> joinedCircleByInferredTopic =
            inferredInterests(statedTopicIds, joinedCircleIds, allCircles);

        List<CircleCandidate> ranked = allCircles.stream()
            .filter(circle -> isEligible(circle, joinedCircleIds))
            .sorted(Comparator
                .comparing((CircleCandidate c) -> matchType(c, statedTopicIds, joinedCircleByInferredTopic))
                .thenComparing(CircleCandidate::feedPosts30d, Comparator.reverseOrder())
                .thenComparingLong(CircleCandidate::id))
            .toList();

        List<MainPick> picks = groupByTopic(ranked);

        List<Recommendation> recommendations = new ArrayList<>();
        for (int i = 0; i < picks.size() && i < limit; i++) {
            CircleCandidate circle = picks.get(i).circle;
            MatchType matchType = matchType(circle, statedTopicIds, joinedCircleByInferredTopic);
            String reason = reason(circle, matchType, joinedCircleByInferredTopic.get(circle.topicId()));
            recommendations.add(new Recommendation(
                i + 1, circle, matchType, reason, List.copyOf(picks.get(i).alsoOnThisTopic)));
        }
        return new RecommendationResult(statedTopicIds.isEmpty(), recommendations);
    }

    /**
     * Topics of Circles the member already belongs to, minus topics they stated.
     * Maps each inferred topic to one of those Circles (lowest id) so the reason can name it.
     */
    private static Map<Long, CircleCandidate> inferredInterests(
            Set<Long> statedTopicIds, Set<Long> joinedCircleIds, List<CircleCandidate> allCircles) {
        Map<Long, CircleCandidate> byTopic = new HashMap<>();
        allCircles.stream()
            .filter(c -> joinedCircleIds.contains(c.id()))
            .filter(c -> c.topicId() != null && !statedTopicIds.contains(c.topicId()))
            .sorted(Comparator.comparingLong(CircleCandidate::id))
            .forEach(c -> byTopic.putIfAbsent(c.topicId(), c));
        return byTopic;
    }

    private static boolean isEligible(CircleCandidate circle, Set<Long> joinedCircleIds) {
        return !joinedCircleIds.contains(circle.id())
            && !circle.isFull()
            && !circle.isInactive()
            && !circle.isUnlisted();
    }

    private static MatchType matchType(
            CircleCandidate circle, Set<Long> statedTopicIds, Map<Long, CircleCandidate> inferred) {
        if (circle.topicId() != null && statedTopicIds.contains(circle.topicId())) {
            return MatchType.STATED_INTEREST;
        }
        if (circle.topicId() != null && inferred.containsKey(circle.topicId())) {
            return MatchType.INFERRED_INTEREST;
        }
        return MatchType.POPULAR;
    }

    /** Walks the ranked list: the first Circle per topic becomes a main pick, later ones attach to it. */
    private static List<MainPick> groupByTopic(List<CircleCandidate> ranked) {
        List<MainPick> picks = new ArrayList<>();
        Map<Long, MainPick> pickByTopic = new HashMap<>();
        for (CircleCandidate circle : ranked) {
            MainPick existing = circle.topicId() == null ? null : pickByTopic.get(circle.topicId());
            if (existing != null) {
                existing.alsoOnThisTopic.add(circle);
                continue;
            }
            MainPick pick = new MainPick(circle);
            picks.add(pick);
            if (circle.topicId() != null) {
                pickByTopic.put(circle.topicId(), pick);
            }
        }
        return picks;
    }

    /** Member-facing reason: why it fits them, plus a concrete fact showing the Circle is active. */
    static String reason(CircleCandidate circle, MatchType matchType, CircleCandidate joinedCircleOnSameTopic) {
        int posts = circle.feedPosts30d();
        String activity = posts + (posts == 1 ? " post" : " posts") + " in the last 30 days.";
        String reason = switch (matchType) {
            case STATED_INTEREST ->
                "Matches your interest in " + circle.topicLabel() + ". " + activity;
            case INFERRED_INTEREST ->
                "You're in " + joinedCircleOnSameTopic.name() + ", so you may like another "
                    + circle.topicLabel() + " Circle. " + activity;
            case POPULAR ->
                "One of the most active Circles right now: " + activity;
        };
        if (circle.joinPolicy() == JoinPolicy.REQUEST) {
            reason += " The leader approves new members.";
        }
        return reason;
    }

    private static final class MainPick {
        private final CircleCandidate circle;
        private final List<CircleCandidate> alsoOnThisTopic = new ArrayList<>();

        private MainPick(CircleCandidate circle) {
            this.circle = circle;
        }
    }
}
