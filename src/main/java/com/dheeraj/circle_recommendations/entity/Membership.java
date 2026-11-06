package com.dheeraj.circle_recommendations.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** A user's membership in a Circle. A Circle's leaders are members with role LEADER. */
@Entity
@Table(name = "memberships")
public class Membership {

    @EmbeddedId
    private MembershipId id;

    @Convert(converter = MembershipRole.JpaConverter.class)
    private MembershipRole role;

    @Column(name = "joined_at")
    private Instant joinedAt;

    protected Membership() {
        // for JPA
    }

    public MembershipId getId() {
        return id;
    }

    public MembershipRole getRole() {
        return role;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }
}
