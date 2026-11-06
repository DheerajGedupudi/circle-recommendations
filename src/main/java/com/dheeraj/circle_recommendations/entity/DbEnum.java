package com.dheeraj.circle_recommendations.entity;

/**
 * An enum stored in the database (and returned by the API) as a lowercase string,
 * e.g. {@code CircleFormat.IN_PERSON} is stored as {@code "in_person"}.
 */
public interface DbEnum {

    String dbValue();
}
