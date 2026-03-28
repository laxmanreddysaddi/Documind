package com.documind.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            // ✅ ENABLE CORS
            .cors(cors -> {})

            .authorizeHttpRequests(auth -> auth

                // ✅ VERY IMPORTANT (fixes 403 preflight)
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // ✅ your APIs
                .requestMatchers(
                        "/api/auth/**",
                        "/api/documents/**",
                        "/api/chat/**"
                ).permitAll()

                .anyRequest().authenticated()
            );

        return http.build();
    }
}