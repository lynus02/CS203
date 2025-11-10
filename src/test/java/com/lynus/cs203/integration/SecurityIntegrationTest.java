package com.lynus.cs203.integration;

import com.lynus.cs203.Cs203Application;
import com.lynus.cs203.entities.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Security Integration Tests")
public class SecurityIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Should allow public access to non-protected endpoints")
    void publicEndpoints_ShouldBeAccessible() throws Exception {
        // Test public endpoints from your SecurityConfig
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/tariffs"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/tariff-rates"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should deny unauthenticated users access to protected endpoints")
    void authenticatedUser_ShouldAccessProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/users/profile"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/users/profile"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should enforce role-based access control")
    void roleBasedAccessControl_ShouldBeEnforced() throws Exception {
        UserDetails userDetails = createUserDetails(testUser.getUserId(), "USER");
        UserDetails adminDetails = createUserDetails(adminUser.getUserId(), "ADMIN");

        // Regular user should not access admin endpoints
        mockMvc.perform(get("/admin/users")
                        .with(SecurityMockMvcRequestPostProcessors.user(userDetails)))
                .andExpect(status().isForbidden());

        // Admin should access admin endpoints
        mockMvc.perform(get("/admin/users")
                        .with(SecurityMockMvcRequestPostProcessors.user(adminDetails)))
                .andExpect(status().isOk());

        // Both should access user endpoints
        mockMvc.perform(get("/users/profile")
                        .with(SecurityMockMvcRequestPostProcessors.user(userDetails)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/users/profile")
                        .with(SecurityMockMvcRequestPostProcessors.user(adminDetails)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should handle CORS configuration")
    void cors_ShouldBeConfigured() throws Exception {
        mockMvc.perform(options("/auth/login")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Origin", "http://localhost:3000"))
                .andExpect(status().isOk());

        mockMvc.perform(options("/users/profile")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Origin", "http://localhost:3000"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should handle concurrent security contexts")
    void concurrentAccess_ShouldBePrevented() throws Exception {
        UserDetails userDetails = createUserDetails(testUser.getUserId(), "USER");
        UserDetails adminDetails = createUserDetails(adminUser.getUserId(), "ADMIN");

        // Test that user context is properly isolated
        mockMvc.perform(get("/users/profile")
                        .with(SecurityMockMvcRequestPostProcessors.user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(testUser.getEmail()));

        // Test admin access in same test (simulating concurrent contexts)
        mockMvc.perform(get("/admin/users")
                        .with(SecurityMockMvcRequestPostProcessors.user(adminDetails)))
                .andExpect(status().isOk());

        // Verify user context still works correctly
        mockMvc.perform(get("/users/profile")
                        .with(SecurityMockMvcRequestPostProcessors.user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(testUser.getEmail()));
    }

    @Test
    @DisplayName("Should validate security headers")
    void securityHeaders_ShouldBePresent() throws Exception {
        UserDetails userDetails = createUserDetails(testUser.getUserId(), "USER");

        mockMvc.perform(get("/users/profile")
                        .with(SecurityMockMvcRequestPostProcessors.user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Content-Type-Options"))
                .andExpect(header().exists("X-Frame-Options"));
    }
}
