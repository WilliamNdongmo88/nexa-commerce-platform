package com.microservices.auth.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        String issuer,
        String audience,
        long accessTokenTtl,
        long refreshTokenTtl
) {}