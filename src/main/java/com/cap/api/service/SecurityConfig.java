package com.cap.api.service;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Only enforce HTTPS if not running in test profile
        if (!isTestProfileActive()) {
            http.requiresChannel(channel -> channel.anyRequest().requiresSecure());
        }
        http
            .sessionManagement(session -> {
                session
                    .maximumSessions(1)
                    .maxSessionsPreventsLogin(false);
                session
                    .sessionFixation(sessionFixation -> sessionFixation.migrateSession())
                    .invalidSessionUrl("/login?invalid-session=true");
            });
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(new AntPathRequestMatcher("/mcp/sync")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/logout")).permitAll()
                .anyRequest().authenticated()
            );
        http
            .oauth2Login();
        http
            .logout(logout -> logout
                .logoutUrl("/logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessUrl("/")
            );
        http
            .csrf();
        return http.build();
    }

    private boolean isTestProfileActive() {
        String[] profiles = org.springframework.core.env.AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME
            .split(",");
        String activeProfiles = System.getProperty("spring.profiles.active", "");
        return activeProfiles.contains("test");
    }
}
