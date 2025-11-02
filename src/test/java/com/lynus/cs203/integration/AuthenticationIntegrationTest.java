package com.lynus.cs203.integration;

import com.lynus.cs203.dtos.request.ChangePasswordRequest;
import com.lynus.cs203.dtos.request.LoginRequest;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Authentication Integration Tests")
public class AuthenticationIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void login_WithValidCredentials_ShouldSucceed() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email(testUser.getEmail())
                .password("UserPassword@123")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    @DisplayName("Should reject login with invalid credentials")
    void login_WithInvalidCredentials_ShouldFail() throws Exception {
        LoginRequest wrongPasswordRequest = LoginRequest.builder()
                .email(testUser.getEmail())
                .password("WrongPassword")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongPasswordRequest)))
                .andExpect(status().isUnauthorized());

        LoginRequest wrongEmailRequest = LoginRequest.builder()
                .email("nonexistent@example.com")
                .password("SomePassword")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongEmailRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should reject login for inactive users")
    void login_WithInactiveUser_ShouldFail() throws Exception {
        testUser.setIsActive(false);
        userRepository.save(testUser);

        LoginRequest loginRequest = LoginRequest.builder()
                .email(testUser.getEmail())
                .password("UserPassword@123")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should change password with valid old password")
    void changePassword_WithValidOldPassword_ShouldSucceed() throws Exception {
        UserDetails userDetails = createUserDetails(testUser.getUserId(), "USER");

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("UserPassword@123")
                .newPassword("NewPassword@123")
                .build();

        mockMvc.perform(post("/users/change-password")
                        .with(SecurityMockMvcRequestPostProcessors.user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully"));
    }

    @Test
    @DisplayName("Should reject password change with wrong old password")
    void changePassword_WithWrongOldPassword_ShouldFail() throws Exception {
        UserDetails userDetails = createUserDetails(testUser.getUserId(), "USER");

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("WrongOldPassword")
                .newPassword("NewPassword@123")
                .build();

        mockMvc.perform(post("/users/change-password")
                        .with(SecurityMockMvcRequestPostProcessors.user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Provided password is incorrect"));
    }

    @Test
    @DisplayName("Should reject weak passwords")
    void changePassword_WithWeakPassword_ShouldFail() throws Exception {
        UserDetails userDetails = createUserDetails(testUser.getUserId(), "USER");

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("UserPassword@123")
                .newPassword("weak")
                .build();

        mockMvc.perform(post("/users/change-password")
                        .with(SecurityMockMvcRequestPostProcessors.user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle JWT token validation")
    void jwtAuthentication_ShouldValidateTokens() throws Exception {
        // Test invalid token
        mockMvc.perform(get("/users/profile")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());

        // Test malformed Authorization header
        mockMvc.perform(get("/users/profile")
                        .header("Authorization", "InvalidFormat"))
                .andExpect(status().isUnauthorized());

        // Test missing Authorization header
        mockMvc.perform(get("/users/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should validate login request format")
    void login_WithInvalidRequest_ShouldFail() throws Exception {
        LoginRequest emptyRequest = LoginRequest.builder()
                .email("")
                .password("")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyRequest)))
                .andExpect(status().isBadRequest());
    }
}
