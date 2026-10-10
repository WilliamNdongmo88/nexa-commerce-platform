package com.microservices.auth.security;

import com.microservices.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class JwtService {

    private final SecretKey secretKey;
    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.secretKey = Keys.hmacShaKeyFor(
                properties.secret().getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Génère un access token JWT signé.
     */
    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(properties.accessTokenTtl());

        List<String> roles = user.getRoles().stream()
                .map(r -> r.getName())
                .collect(Collectors.toList());

        return Jwts.builder()
                .subject(user.getId().toString())
                .issuer(properties.issuer())
                .audience().add(properties.audience()).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .id(UUID.randomUUID().toString())    // jti
                .claim("email", user.getEmail())
                .claim("roles", roles)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Valide et parse un access token.
     */
    public Claims validateAndExtract(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(properties.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getAccessTokenTtlSeconds() {
        return properties.accessTokenTtl();
    }
}