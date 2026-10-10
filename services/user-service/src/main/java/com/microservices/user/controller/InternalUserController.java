package com.microservices.user.controller;

import com.microservices.user.service.UserService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Endpoints internes — non exposés par la Gateway.
 * Utilisé notamment pour créer un profil lors de l'inscription.
 *
 * ⚠️ TEMPORAIRE : à remplacer par un consumer Kafka à l'étape 13.
 */
@RestController
@RequestMapping("/internal/v1/users")
@RequiredArgsConstructor
@Hidden
public class InternalUserController {

    private final UserService userService;

    public record CreateProfileInternalRequest(
            UUID id,
            String email,
            String firstName,
            String lastName
    ) {}

    @PostMapping("/sync")
    public void syncProfile(@RequestBody CreateProfileInternalRequest request) {
        userService.createProfile(
                request.id(),
                request.email(),
                request.firstName(),
                request.lastName()
        );
    }
}