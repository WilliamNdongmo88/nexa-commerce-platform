package com.microservices.user.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String phone,
        LocalDate birthDate,
        String addressLine1,
        String addressLine2,
        String city,
        String postalCode,
        String country,
        String avatarUrl,
        String bio,
        String locale,
        String timezone,
        Instant createdAt,
        Instant updatedAt
) {}