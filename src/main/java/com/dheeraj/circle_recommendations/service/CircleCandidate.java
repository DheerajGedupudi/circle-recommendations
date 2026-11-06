package com.dheeraj.circle_recommendations.service;

import com.dheeraj.circle_recommendations.entity.CircleAccess;
import com.dheeraj.circle_recommendations.entity.CircleFormat;
import com.dheeraj.circle_recommendations.entity.JoinPolicy;

/**
 * The facts about a Circle that the recommender needs. A plain record (not a JPA entity),
 * so the ranking logic can be unit tested without a database.
 *
 * @param topicId    null for Circles without a topic
 * @param topicLabel display name of the topic, null when topicId is null
 * @param maxMembers null means no limit
 */
public record CircleCandidate(
    long id,
    String name,
    Long topicId,
    String topicLabel,
    CircleFormat format,
    CircleAccess access,
    JoinPolicy joinPolicy,
    Integer maxMembers,
    int memberCount,
    int feedPosts30d) {

    /** At capacity: no one else can join. */
    public boolean isFull() {
        return maxMembers != null && memberCount >= maxMembers;
    }

    /** No feed posts in the last 30 days. */
    public boolean isInactive() {
        return feedPosts30d == 0;
    }

    /** Leaders chose not to be discoverable, so the Circle is never recommended. */
    public boolean isUnlisted() {
        return access == CircleAccess.UNLISTED;
    }
}
