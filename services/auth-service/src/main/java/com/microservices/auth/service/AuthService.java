package com.microservices.auth.service;

import com.microservices.auth.dto.request.LoginRequest;
import com.microservices.auth.dto.request.RefreshTokenRequest;
import com.microservices.auth.dto.request.RegisterRequest;
import com.microservices.auth.dto.response.AuthResponse;
import com.microservices.auth.dto.response.UserResponse;
import com.microservices.auth.entity.RefreshToken;
import com.microservices.auth.entity.Role;
import com.microservices.auth.entity.User;
import com.microservices.auth.event.AuthEventPublisher;
import com.microservices.auth.exception.EmailAlreadyExistsException;
import com.microservices.auth.exception.InvalidCredentialsException;
import com.microservices.auth.exception.InvalidTokenException;
import com.microservices.auth.mapper.UserMapper;
import com.microservices.auth.repository.RefreshTokenRepository;
import com.microservices.auth.repository.RoleRepository;
import com.microservices.auth.repository.UserRepository;
import com.microservices.auth.security.JwtService;
import com.microservices.auth.security.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final UserMapper userMapper;
    private final RoleRepository roleRepository;
    private final AuthEventPublisher authEventPublisher;

    private static final String DEFAULT_ROLE = "ROLE_CUSTOMER";

    // ============================================================
    // REGISTER
    // ============================================================
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        Role defaultRole = roleRepository.findByName(DEFAULT_ROLE)
                .orElseThrow(() -> new IllegalStateException("Default role not found: " + DEFAULT_ROLE));

        User user = User.builder()
                .email(request.email().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .enabled(true)
                .roles(Set.of(defaultRole))
                .build();

        // ⚠️ Astuce : le Role.builder() ne persiste pas tout seul.
        // Il faut que le rôle existe déjà en base (seed Flyway).
        // On va plutôt chercher le rôle existant.
        //user.getRoles().clear();

        User savedUser = userRepository.save(user);
        authEventPublisher.publishUserRegistered(savedUser);
        log.info("User registered: {}", savedUser.getEmail());

        // TODO : publier event Kafka "auth.user.registered" (étape 13)

        return buildAuthResponse(savedUser);
    }

    // ============================================================
    // LOGIN
    // ============================================================
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().toLowerCase().trim())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        if (!user.getEnabled()) {
            throw new InvalidCredentialsException();
        }

        log.info("User logged in: {}", user.getEmail());
        return buildAuthResponse(user);
    }

    // ============================================================
    // REFRESH
    // ============================================================
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository
                .findByToken(request.refreshToken())
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        if (!refreshToken.isValid()) {
            throw new InvalidTokenException("Refresh token expired or revoked");
        }

        // Rotation : on révoque l'ancien et on en crée un nouveau
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        User user = refreshToken.getUser();
        log.info("Token refreshed for user: {}", user.getEmail());

        return buildAuthResponse(user);
    }

    // ============================================================
    // LOGOUT
    // ============================================================
    @Transactional
    public void logout(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
        log.info("All refresh tokens revoked for user {}", userId);
    }

    // ============================================================
    // HELPERS
    // ============================================================
    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = generateRefreshToken(user);
        UserResponse userResponse = userMapper.toResponse(user);

        return AuthResponse.of(
                accessToken,
                refreshToken,
                jwtService.getAccessTokenTtlSeconds(),
                userResponse
        );
    }

    private String generateRefreshToken(User user) {
        String token = UUID.randomUUID().toString() + "-" + UUID.randomUUID();
        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .user(user)
                .expiresAt(Instant.now().plusSeconds(jwtProperties.refreshTokenTtl()))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);
        return token;
    }
}