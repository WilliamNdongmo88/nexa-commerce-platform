package com.microservices.user.controller;

import com.microservices.user.dto.request.UpdateProfileRequest;
import com.microservices.user.dto.response.UserProfileResponse;
import com.microservices.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Gestion des profils utilisateurs")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Récupère le profil de l'utilisateur courant")
    public UserProfileResponse getMyProfile(@RequestHeader("X-User-Id") UUID userId) {
        return userService.getProfile(userId);
    }

    @PutMapping("/me")
    @Operation(summary = "Met à jour le profil de l'utilisateur courant")
    public UserProfileResponse updateMyProfile(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return userService.updateProfile(userId, request);
    }

    @DeleteMapping("/me")
    @Operation(summary = "Supprime le profil de l'utilisateur courant")
    public ResponseEntity<Void> deleteMyProfile(@RequestHeader("X-User-Id") UUID userId) {
        userService.deleteProfile(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupère un profil par ID (admin / interne)")
    public UserProfileResponse getProfile(@PathVariable UUID id) {
        return userService.getProfile(id);
    }
}