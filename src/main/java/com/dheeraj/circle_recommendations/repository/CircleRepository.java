package com.dheeraj.circle_recommendations.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.dheeraj.circle_recommendations.entity.Circle;

public interface CircleRepository extends JpaRepository<Circle, Long> {

    /**
     * Loads every Circle with its topic in one query (no N+1).
     * Fine for 40 Circles; at scale, candidates would be queried by topic instead
     * (backed by the circles(topic_id) index).
     */
    @Query("select c from Circle c left join fetch c.topic")
    List<Circle> findAllWithTopic();
}
