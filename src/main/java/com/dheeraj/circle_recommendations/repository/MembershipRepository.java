package com.dheeraj.circle_recommendations.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dheeraj.circle_recommendations.entity.Membership;
import com.dheeraj.circle_recommendations.entity.MembershipId;

public interface MembershipRepository extends JpaRepository<Membership, MembershipId> {

    /** Ids of the Circles a user belongs to (served by the (user_id, circle_id) primary key). */
    @Query("select m.id.circleId from Membership m where m.id.userId = :userId")
    List<Long> findCircleIdsByUserId(@Param("userId") long userId);
}
