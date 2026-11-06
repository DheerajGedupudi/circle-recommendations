package com.dheeraj.circle_recommendations.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dheeraj.circle_recommendations.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    /** Loads the user and their stated interests in one query. */
    @Query("select u from User u left join fetch u.topicInterests where u.id = :id")
    Optional<User> findWithInterestsById(@Param("id") long id);
}
