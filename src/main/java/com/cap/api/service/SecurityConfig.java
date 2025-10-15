package com.cap.api.service;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
// import org.springframework.security.web.util.matcher.AntPathRequestMatcher; // unused

import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Note: requiresChannel()/requiresSecure() were deprecated in later Spring Security versions.
        // For this demo we avoid enforcing HTTPS programmatically to reduce deprecation warnings.
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
                .requestMatchers("/mcp/sync").permitAll()
                .requestMatchers("/logout").permitAll()
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

    @SuppressWarnings("unused")
    private boolean isTestProfileActive() {
        String[] profiles = org.springframework.core.env.AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME
            .split(",");
        String activeProfiles = System.getProperty("spring.profiles.active", "");
        return activeProfiles.contains("test");
    }
}
