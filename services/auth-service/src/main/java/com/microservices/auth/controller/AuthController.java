package com.microservices.auth.controller;

import com.microservices.auth.dto.request.LoginRequest;
import com.microservices.auth.dto.request.RefreshTokenRequest;
import com.microservices.auth.dto.request.RegisterRequest;
import com.microservices.auth.dto.response.AuthResponse;
import com.microservices.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints d'authentification")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Inscription d'un nouvel utilisateur")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Connexion utilisateur")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renouvellement d'un access token via refresh token")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    @Operation(summary = "Déconnexion — révoque tous les refresh tokens de l'utilisateur")
    public ResponseEntity<Void> logout(@RequestHeader("X-User-Id") UUID userId) {
        authService.logout(userId);
        return ResponseEntity.noContent().build();
    }
}