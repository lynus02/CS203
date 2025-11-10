package com.lynus.cs203.integration.controllers;

import com.lynus.cs203.Cs203Application;
import com.lynus.cs203.dtos.request.ChangePasswordRequest;
import com.lynus.cs203.dtos.request.CreateUserRequest;
import com.lynus.cs203.dtos.request.UpdateUserRequest;
import com.lynus.cs203.entities.User;
import com.lynus.cs203.entities.UserProfile;
import com.lynus.cs203.integration.BaseIntegrationTest;
import com.lynus.cs203.repositories.UserProfileRepository;
import com.lynus.cs203.repositories.UserRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("User Controller Integration Tests")
public class UserControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Should create a new user successfully")
    void createUser_ShouldReturnCreatedUser() throws Exception {
        CreateUserRequest createUserRequest = CreateUserRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("johndoe@example.com")
                .password("Password@123")
                .build();

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email", is("johndoe@example.com")))
                .andExpect(jsonPath("$.firstName", is("John")))
                .andExpect(jsonPath("$.lastName", is("Doe")))
                .andExpect(jsonPath("$.userId", notNullValue()));

        List<User> users = userRepository.findAll();
        assertThat(users).hasSize(3); // testUser, adminUser, and new user
        assertThat(users.stream().anyMatch(u -> u.getEmail().equals("johndoe@example.com"))).isTrue();
    }

    @Test
    @DisplayName("Should return 409 when creating a user with existing email")
    void createUser_WithExistingEmail_ShouldReturnConflict() throws Exception {
        CreateUserRequest createUserRequest = CreateUserRequest.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email(testUser.getEmail()) // Using existing email
                .password("Password@123")
                .build();

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Email already exists")));

        List<User> users = userRepository.findAll();
        Assertions.assertThat(users).hasSize(2); // Only testUser and adminUser
    }

    @Test
    @DisplayName("Should get current user profile when authenticated")
    void getCurrentUserProfile_ShouldReturnUserProfile_WhenAuthenticated() throws Exception {
        UserDetails userDetails = createUserDetails(testUser.getUserId(), "USER");

        mockMvc.perform(get("/users/profile")
                        .with(SecurityMockMvcRequestPostProcessors.user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(testUser.getUserId())))
                .andExpect(jsonPath("$.email", is(testUser.getEmail())))
                .andExpect(jsonPath("$.firstName", is("Test")))
                .andExpect(jsonPath("$.lastName", is("User")));
    }

    @Test
    @DisplayName("Should return 404 when getting profile for non-existent user")
    void getCurrentUserProfile_ShouldReturn404_WhenUserNotFound() throws Exception {
        UserDetails userDetails = createUserDetails("nonexistent123", "USER");

        mockMvc.perform(get("/users/profile")
                        .with(SecurityMockMvcRequestPostProcessors.user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("User not found")));
    }

    @Test
    @DisplayName("Should update current user profile successfully")
    @WithMockUser(username = "user123")
    void updateCurrentUserProfile_ShouldReturnUpdatedProfile() throws Exception {
        UserDetails userDetails = createUserDetails(testUser.getUserId(), "USER");

        UpdateUserRequest updateRequest = UpdateUserRequest.builder()
                .email("updated@example.com")
                .firstName("UpdatedFirst")
                .lastName("UpdatedLast")
                .build();

        mockMvc.perform(put("/users/profile")
                        .with(SecurityMockMvcRequestPostProcessors.user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("updated@example.com")))
                .andExpect(jsonPath("$.firstName", is("UpdatedFirst")))
                .andExpect(jsonPath("$.lastName", is("UpdatedLast")));

        User updatedUser = userRepository.findById(testUser.getUserId()).orElseThrow();
        Assertions.assertThat(updatedUser.getEmail()).isEqualTo("updated@example.com");
        Assertions.assertThat(updatedUser.getUserProfile().getFirstName()).isEqualTo("UpdatedFirst");
    }

    @Test
    @DisplayName("Should return 409 when updating profile with existing email")
    void updateCurrentUserProfile_ShouldReturnConflict_WhenEmailExists() throws Exception {
        UserDetails userDetails = createUserDetails(testUser.getUserId(), "USER");

        UpdateUserRequest updateRequest = UpdateUserRequest.builder()
                .email(adminUser.getEmail()) // Using admin's email
                .build();

        mockMvc.perform(put("/users/profile")
                        .with(SecurityMockMvcRequestPostProcessors.user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Email already exists")));

        User unchangedUser = userRepository.findById(testUser.getUserId()).orElseThrow();
        Assertions.assertThat(unchangedUser.getEmail()).isEqualTo(testUser.getEmail());
    }

    @Test
    @DisplayName("Should delete current user successfully")
    void deleteCurrentUser_ShouldReturnNoContent() throws Exception {
        UserDetails userDetails = createUserDetails(testUser.getUserId(), "USER");

        mockMvc.perform(delete("/users/profile")
                        .with(SecurityMockMvcRequestPostProcessors.user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findById(testUser.getUserId())).isEmpty();
    }

    @Test
    @DisplayName("Should validate user creation data")
    void createUser_WithInvalidData_ShouldFail() throws Exception {
        CreateUserRequest invalidRequest = CreateUserRequest.builder()
                .firstName("") // Invalid: empty
                .lastName("D") // Invalid: too short
                .email("invalid-email") // Invalid: bad format
                .password("weak") // Invalid: too weak
                .build();

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}
