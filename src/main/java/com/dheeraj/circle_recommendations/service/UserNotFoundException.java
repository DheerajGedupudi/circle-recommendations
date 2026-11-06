package com.dheeraj.circle_recommendations.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Rendered as a 404 problem-details response. */
public class UserNotFoundException extends ResponseStatusException {

    public UserNotFoundException(long userId) {
        super(HttpStatus.NOT_FOUND, "User " + userId + " not found");
    }
}
