package com.microservices.user.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateProfileRequest(
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Pattern(regexp = "^\\+?[0-9\\s\\-()]{6,30}$", message = "Invalid phone format")
        String phone,
        LocalDate birthDate,
        @Size(max = 255) String addressLine1,
        @Size(max = 255) String addressLine2,
        @Size(max = 100) String city,
        @Size(max = 20) String postalCode,
        @Size(max = 100) String country,
        @Size(max = 500) String avatarUrl,
        @Size(max = 2000) String bio,
        @Size(max = 10) String locale,
        @Size(max = 50) String timezone
) {}