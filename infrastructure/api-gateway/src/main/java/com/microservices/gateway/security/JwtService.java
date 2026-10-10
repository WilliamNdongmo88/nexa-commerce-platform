package com.microservices.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

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
     * Valide un token et retourne les claims.
     * Lance une exception si le token est invalide ou expiré.
     */
    public Claims validateAndExtract(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(properties.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUserId(Claims claims) {
        return claims.getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(Claims claims) {
        Object roles = claims.get("roles");
        return roles instanceof List ? (List<String>) roles : List.of();
    }

    public String extractEmail(Claims claims) {
        return claims.get("email", String.class);
    }

    public boolean isExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }
}