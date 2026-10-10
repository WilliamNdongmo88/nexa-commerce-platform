package com.microservices.user.exception;

import java.util.UUID;

public class UserProfileNotFoundException extends RuntimeException {
    public UserProfileNotFoundException(UUID id) {
        super("User profile not found: " + id);
    }

    public UserProfileNotFoundException(String email) {
        super("User profile not found for email: " + email);
    }
}