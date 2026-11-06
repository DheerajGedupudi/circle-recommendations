package com.dheeraj.circle_recommendations.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Composite primary key of a membership: one row per (user, Circle) pair. */
@Embeddable
public class MembershipId implements Serializable {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "circle_id")
    private Long circleId;

    protected MembershipId() {
        // for JPA
    }

    public MembershipId(Long userId, Long circleId) {
        this.userId = userId;
        this.circleId = circleId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getCircleId() {
        return circleId;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof MembershipId that
            && Objects.equals(userId, that.userId)
            && Objects.equals(circleId, that.circleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, circleId);
    }
}
