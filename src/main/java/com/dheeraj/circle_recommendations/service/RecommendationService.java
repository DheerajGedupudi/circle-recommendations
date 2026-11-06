package com.dheeraj.circle_recommendations.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dheeraj.circle_recommendations.entity.Circle;
import com.dheeraj.circle_recommendations.entity.Topic;
import com.dheeraj.circle_recommendations.entity.User;
import com.dheeraj.circle_recommendations.repository.CircleRepository;
import com.dheeraj.circle_recommendations.repository.MembershipRepository;
import com.dheeraj.circle_recommendations.repository.UserRepository;

/**
 * Loads what the recommender needs in three bulk queries (user + interests, the user's
 * memberships, all Circles + topics), then hands plain data to {@link Recommender}.
 */
@Service
public class RecommendationService {

    private final UserRepository users;
    private final MembershipRepository memberships;
    private final CircleRepository circles;
    private final Recommender recommender = new Recommender();

    public RecommendationService(UserRepository users, MembershipRepository memberships, CircleRepository circles) {
        this.users = users;
        this.memberships = memberships;
        this.circles = circles;
    }

    @Transactional(readOnly = true)
    public RecommendationResult recommend(long userId, int limit) {
        User user = users.findWithInterestsById(userId).orElseThrow(() -> new UserNotFoundException(userId));

        Set<Long> statedTopicIds = user.getTopicInterests().stream()
            .map(Topic::getId)
            .collect(Collectors.toSet());
        Set<Long> joinedCircleIds = new HashSet<>(memberships.findCircleIdsByUserId(userId));
        List<CircleCandidate> allCircles = circles.findAllWithTopic().stream()
            .map(RecommendationService::toCandidate)
            .toList();

        return recommender.recommend(statedTopicIds, joinedCircleIds, allCircles, limit);
    }

    private static CircleCandidate toCandidate(Circle circle) {
        Topic topic = circle.getTopic();
        return new CircleCandidate(
            circle.getId(),
            circle.getName(),
            topic == null ? null : topic.getId(),
            topic == null ? null : topic.getLabel(),
            circle.getFormat(),
            circle.getAccess(),
            circle.getJoinPolicy(),
            circle.getMaxMembers(),
            circle.getMemberCount(),
            circle.getFeedPosts30d());
    }
}
