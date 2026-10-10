package com.microservices.eureka.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Désactive CSRF (Eureka est un service interne, pas un site web)
                .csrf(AbstractHttpConfigurer::disable)

                // Autorise les requêtes internes d'enregistrement / heartbeat
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .anyRequest().authenticated()
                )

                // Active HTTP Basic (les clients Eureka s'authentifient via l'URL)
                .httpBasic(basic -> {});

        return http.build();
    }
}