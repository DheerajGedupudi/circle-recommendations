package com.dheeraj.circle_recommendations.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A Circle. Only the fields the recommender reads are mapped.
 * "Is it full?" is derived from maxMembers and memberCount rather than stored.
 */
@Entity
@Table(name = "circles")
public class Circle {

    @Id
    private Long id;

    private String name;

    /** Null for Circles without a topic. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Convert(converter = CircleFormat.JpaConverter.class)
    private CircleFormat format;

    @Convert(converter = CircleAccess.JpaConverter.class)
    private CircleAccess access;

    @Convert(converter = JoinPolicy.JpaConverter.class)
    @Column(name = "join_policy")
    private JoinPolicy joinPolicy;

    /** Null means no limit. */
    @Column(name = "max_members")
    private Integer maxMembers;

    @Column(name = "member_count")
    private int memberCount;

    @Column(name = "feed_posts_30d")
    private int feedPosts30d;

    protected Circle() {
        // for JPA
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Topic getTopic() {
        return topic;
    }

    public CircleFormat getFormat() {
        return format;
    }

    public CircleAccess getAccess() {
        return access;
    }

    public JoinPolicy getJoinPolicy() {
        return joinPolicy;
    }

    public Integer getMaxMembers() {
        return maxMembers;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public int getFeedPosts30d() {
        return feedPosts30d;
    }
}
