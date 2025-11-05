package com.lynus.cs203.config;

import com.lynus.cs203.entities.Role;
import com.lynus.cs203.filters.JwtAuthenticationFilter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Slf4j
@Configuration
@EnableWebSecurity
@AllArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public PasswordEncoder passwordEncoder() {
        log.debug("Creating BCryptPasswordEncoder bean");
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        log.debug("Creating AuthenticationManager bean");
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("Creating SecurityFilterChain bean");
        // Stateless sessions (token-based authentication)
        // Disable CSRF (cross site request forgery)
        // Authorize requests
        http
                .sessionManagement(c -> {
                    log.debug("Configuring stateless session management");
                    c.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
                })
                .cors(cors -> {
                    log.debug("Configuring CORS");
                    cors.configurationSource(corsConfigurationSource);
                })
                .csrf(csrf -> {
                    log.debug("Disabling CSRF protection for stateless API");
                    csrf.disable();
                })
                .authorizeHttpRequests(c -> {
                    log.info("Configuring authorization rules");

                    c.requestMatchers("/").permitAll()
                            // Authentication endpoints = allow anyone to register/login
                            .requestMatchers(HttpMethod.POST, "/users").permitAll()
                            .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                            .requestMatchers(HttpMethod.POST, "/auth/refresh").permitAll()

                            // Public endpoints
                            .requestMatchers("/tariffs/**").permitAll()
                            .requestMatchers("/products/**").permitAll()
                            .requestMatchers("/tariff-rates/**").permitAll()
                            .requestMatchers("/").permitAll()

                            // Swagger and API docs
                            .requestMatchers("/v3/api-docs/**").permitAll()
                            .requestMatchers("/swagger-ui/**").permitAll()

                            // User Management endpoints
                            .requestMatchers("/users/**").authenticated()
                            .requestMatchers("/auth/me").authenticated()

                            // Admin endpoints
                            .requestMatchers("/admin/**").hasRole(Role.ADMIN.name())

                            // All other endpoints require authentication
                            .anyRequest().authenticated();

                    log.debug("Authorization rules configured successfully");
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(c -> {
                    log.debug("Configuring exception handling");
                    c.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                            .accessDeniedHandler(accessDeniedHandler());
                });

        log.info("SecurityFilterChain configured successfully");
        return http.build();
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            String user = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "anonymous";
            log.warn("Access denied for user '{}' attempting {} {}",
                    user, request.getMethod(), request.getRequestURI());

            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"Access denied\",\"message\":\"You don't have permission to access this resource\"}"
            );
        };
    }
}
