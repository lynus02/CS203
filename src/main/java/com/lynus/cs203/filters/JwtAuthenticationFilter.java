package com.lynus.cs203.filters;

import com.lynus.cs203.services.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws IOException, ServletException {
        log.debug("Processing authentication for request: {} {}", request.getMethod(), request.getRequestURI());

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No Bearer token found, skipping JWT authentication");
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        log.debug("Extracted JWT token from Authorization header");

        try {
            // Validate token BEFORE extracting user ID
            if (!jwtService.validateToken(token)) {
                log.warn("Invalid or expired JWT token for request: {} {}", request.getMethod(), request.getRequestURI());
                // Don't set authentication context - let Spring Security handle it
                filterChain.doFilter(request, response);
                return;
            }

            String userId = jwtService.extractUserId(token);

            // Only set authentication if we don't already have one and token is valid
            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                log.debug("JWT token validated successfully for user: {}", userId);

                // Get user roles from JWT token
                List<String> roles = jwtService.extractRoles(token);
                List<GrantedAuthority> authorities = roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList());

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId, null, authorities);

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("Authentication set in SecurityContext for user: {} with roles: {}", userId, roles);
            } else if (userId == null) {
                log.warn("Valid JWT token but no user ID extracted");
            }

        } catch (Exception e) {
            log.warn("JWT token processing failed for request: {} {} - {}",
                    request.getMethod(), request.getRequestURI(), e.getMessage());
            log.debug("JWT processing exception details", e);
        }

        filterChain.doFilter(request, response);
    }
}
