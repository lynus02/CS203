package com.lynus.cs203.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lynus.cs203.dtos.request.ChangePasswordRequest;
import com.lynus.cs203.dtos.request.CreateUserRequest;
import com.lynus.cs203.dtos.request.UpdateUserRequest;
import com.lynus.cs203.entities.*;
import com.lynus.cs203.repositories.UserProfileRepository;
import com.lynus.cs203.repositories.UserRepository;
import com.lynus.cs203.repositories.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("User Management Integration Tests")
public class UserManagementIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Admin should get all users")
    void getAllUsers_WhenAdmin_ShouldSucceed() throws Exception {
        UserDetails adminDetails = createUserDetails(adminUser.getUserId(), "ADMIN");

        mockMvc.perform(get("/admin/users")
                        .with(SecurityMockMvcRequestPostProcessors.user(adminDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2))) // testUser and adminUser
                .andExpect(jsonPath("$[0].email").exists())
                .andExpect(jsonPath("$[0].firstName").exists());
    }

    @Test
    @DisplayName("Admin should get user by ID")
    void getUserById_WhenAdmin_ShouldSucceed() throws Exception {
        UserDetails adminDetails = createUserDetails(adminUser.getUserId(), "ADMIN");

        mockMvc.perform(get("/admin/users/{id}", testUser.getUserId())
                        .with(SecurityMockMvcRequestPostProcessors.user(adminDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(testUser.getUserId()))
                .andExpect(jsonPath("$.email").value(testUser.getEmail()))
                .andExpect(jsonPath("$.firstName").value("Test"));
    }

    @Test
    @DisplayName("Admin should assign roles to users")
    void assignRole_WhenAdmin_ShouldSucceed() throws Exception {
        UserDetails adminDetails = createUserDetails(adminUser.getUserId(), "ADMIN");

        mockMvc.perform(post("/admin/users/{id}/roles/{roleName}", testUser.getUserId(), "ADMIN")
                        .with(SecurityMockMvcRequestPostProcessors.user(adminDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Role assigned successfully"))
                .andExpect(jsonPath("$.userId").value(testUser.getUserId()))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("Admin should remove roles from users")
    void removeRole_WhenAdmin_ShouldSucceed() throws Exception {
        UserDetails adminDetails = createUserDetails(adminUser.getUserId(), "ADMIN");

        // First assign a role
        mockMvc.perform(post("/admin/users/{id}/roles/{roleName}", testUser.getUserId(), "ADMIN")
                        .with(SecurityMockMvcRequestPostProcessors.user(adminDetails)))
                .andExpect(status().isOk());

        // Then remove it
        mockMvc.perform(delete("/admin/users/{id}/roles/{roleName}", testUser.getUserId(), "ADMIN")
                        .with(SecurityMockMvcRequestPostProcessors.user(adminDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Role removed successfully"))
                .andExpect(jsonPath("$.userId").value(testUser.getUserId()));
    }

    @Test
    @DisplayName("Admin should get user roles")
    void getUserRoles_WhenAdmin_ShouldSucceed() throws Exception {
        UserDetails adminDetails = createUserDetails(adminUser.getUserId(), "ADMIN");

        mockMvc.perform(get("/admin/users/{id}/roles", testUser.getUserId())
                        .with(SecurityMockMvcRequestPostProcessors.user(adminDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(testUser.getUserId()))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles", hasSize(1)))
                .andExpect(jsonPath("$.roles[0]").value("USER"));
    }

    @Test
    @DisplayName("Regular users should not access admin endpoints")
    void adminEndpoints_WhenRegularUser_ShouldFail() throws Exception {
        UserDetails userDetails = createUserDetails(testUser.getUserId(), "USER");

        mockMvc.perform(get("/admin/users")
                        .with(SecurityMockMvcRequestPostProcessors.user(userDetails)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/users/{id}", testUser.getUserId())
                        .with(SecurityMockMvcRequestPostProcessors.user(userDetails)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/admin/users/{id}/roles/{roleName}", testUser.getUserId(), "ADMIN")
                        .with(SecurityMockMvcRequestPostProcessors.user(userDetails)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/admin/users/{id}/roles/{roleName}", testUser.getUserId(), "USER")
                        .with(SecurityMockMvcRequestPostProcessors.user(userDetails)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should handle invalid role operations")
    void invalidRoleOperations_ShouldFail() throws Exception {
        UserDetails adminDetails = createUserDetails(adminUser.getUserId(), "ADMIN");

        // Invalid role name
        mockMvc.perform(post("/admin/users/{id}/roles/{roleName}", testUser.getUserId(), "INVALID_ROLE")
                        .with(SecurityMockMvcRequestPostProcessors.user(adminDetails)))
                .andExpect(status().isBadRequest());

        // Non-existent user
        mockMvc.perform(post("/admin/users/{id}/roles/{roleName}", "non-existent-id", "USER")
                        .with(SecurityMockMvcRequestPostProcessors.user(adminDetails)))
                .andExpect(status().isNotFound());
    }
}
