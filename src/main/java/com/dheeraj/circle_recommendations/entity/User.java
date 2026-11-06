package com.dheeraj.circle_recommendations.entity;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

/**
 * A member. Only the fields the API reads are mapped; the profile fields stay in the
 * users table for future use (e.g. matching on industry or location).
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    private Long id;

    @Column(name = "first_name")
    private String firstName;

    @ManyToMany
    @JoinTable(
        name = "user_topic_interests",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "topic_id"))
    private Set<Topic> topicInterests = new HashSet<>();

    protected User() {
        // for JPA
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public Set<Topic> getTopicInterests() {
        return topicInterests;
    }
}
