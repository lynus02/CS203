package com.lynus.cs203.integration;

import com.lynus.cs203.dtos.request.ChangePasswordRequest;
import com.lynus.cs203.dtos.request.CreateUserRequest;
import com.lynus.cs203.dtos.request.UpdateUserRequest;
import com.lynus.cs203.entities.User;
import com.lynus.cs203.entities.UserProfile;
import com.lynus.cs203.repositories.UserProfileRepository;
import com.lynus.cs203.repositories.UserRepository;
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
public class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
    }

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
        assert users.size() == 1;
        assert users.get(0).getEmail().equals("johndoe@example.com");
    }

    @Test
    @DisplayName("Should return 409 when creating a user with existing email")
    void createUser_WithExistingEmail_ShouldReturnConflict() throws Exception {
        User existingUser = new User();
        existingUser.setEmail("test@example.com");
        existingUser.setPassword(passwordEncoder.encode("Password@123"));
        existingUser.setIsActive(true);
        existingUser.setCreatedAt(java.time.Instant.now());
        existingUser.setUpdatedAt(java.time.Instant.now());

        UserProfile existingProfile = new UserProfile();
        existingProfile.setFirstName("Existing");
        existingProfile.setLastName("User");
        existingProfile.setUser(existingUser);

        existingUser.setUserProfile(existingProfile);

        userRepository.saveAndFlush(existingUser);

        CreateUserRequest createUserRequest = CreateUserRequest.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("test@example.com")
                .password("Password@123")
                .build();

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Email already exists")));

        List<User> users = userRepository.findAll();
        assertThat(users).hasSize(1);
        assertThat(users.get(0).getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should get current user profile when authenticated")
    void getCurrentUserProfile_ShouldReturnUserProfile_WhenAuthenticated() throws Exception {
        String testEmail = "johndoe@example.com";

        User savedUser = new User();
        savedUser.setEmail(testEmail);
        savedUser.setPassword(passwordEncoder.encode("Password@123"));
        savedUser.setIsActive(true);
        savedUser.setCreatedAt(java.time.Instant.now());
        savedUser.setUpdatedAt(java.time.Instant.now());

        UserProfile savedProfile = new UserProfile();
        savedProfile.setFirstName("John");
        savedProfile.setLastName("Doe");
        savedProfile.setUser(savedUser);

        savedUser.setUserProfile(savedProfile);
        User persistedUser = userRepository.saveAndFlush(savedUser);
        String autoGeneratedUserId = persistedUser.getUserId();

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                        .username(autoGeneratedUserId)
                        .password("Password@123")
                        .roles("USER")
                        .build();

        mockMvc.perform(get("/users/profile")
                .with(SecurityMockMvcRequestPostProcessors.user(userDetails))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(autoGeneratedUserId)))
                .andExpect(jsonPath("$.email", is(testEmail)))
                .andExpect(jsonPath("$.firstName", is("John")))
                .andExpect(jsonPath("$.lastName", is("Doe")));
    }

    @Test
    @DisplayName("Should return 404 when getting profile for non-existent user")
    void getCurrentUserProfile_ShouldReturn404_WhenUserNotFound() throws Exception {
        String nonExistentUserId = "nonexistent123";

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(nonExistentUserId)
                .password("Password@123")
                .roles("USER")
                .build();

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
        User existingUser = new User();
        existingUser.setEmail("test@example.com");
        existingUser.setPassword(passwordEncoder.encode("Password@123"));
        existingUser.setIsActive(true);
        existingUser.setCreatedAt(java.time.Instant.now());
        existingUser.setUpdatedAt(java.time.Instant.now());

        UserProfile existingProfile = new UserProfile();
        existingProfile.setFirstName("Existing");
        existingProfile.setLastName("User");
        existingProfile.setUser(existingUser);

        existingUser.setUserProfile(existingProfile);

        userRepository.saveAndFlush(existingUser);

        UpdateUserRequest updateUserRequest = UpdateUserRequest.builder()
                .email("updated@example.com")
                .firstName("UpdatedFirstName")
                .lastName("UpdatedLastName")
                .build();

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(existingUser.getUserId())
                .password("Password@123")
                .roles("USER")
                .build();

        mockMvc.perform(put("/users/profile")
                .with(SecurityMockMvcRequestPostProcessors.user(userDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateUserRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("updated@example.com")))
                .andExpect(jsonPath("$.firstName", is("UpdatedFirstName")))
                .andExpect(jsonPath("$.lastName", is("UpdatedLastName")));

        User updatedUser = userRepository.findById(existingUser.getUserId()).orElseThrow();
        assertThat(updatedUser.getEmail()).isEqualTo("updated@example.com");
        assertThat(updatedUser.getUserProfile().getFirstName()).isEqualTo("UpdatedFirstName");
        assertThat(updatedUser.getUserProfile().getLastName()).isEqualTo("UpdatedLastName");
    }

    @Test
    @DisplayName("Should return 409 when updating profile with existing email")
    void updateCurrentUserProfile_ShouldReturnConflict_WhenEmailExists() throws Exception {
        User user1 = new User();
        user1.setEmail("test@example.com");
        user1.setPassword(passwordEncoder.encode("Password@123"));
        user1.setIsActive(true);
        user1.setCreatedAt(java.time.Instant.now());
        user1.setUpdatedAt(java.time.Instant.now());

        UserProfile user1Profile = new UserProfile();
        user1Profile.setFirstName("Current");
        user1Profile.setLastName("User");
        user1Profile.setUser(user1);
        user1.setUserProfile(user1Profile);

        User user2 = new User();
        user2.setEmail("taken@example.com");
        user2.setPassword(passwordEncoder.encode("Password@123"));
        user2.setIsActive(true);
        user2.setCreatedAt(java.time.Instant.now());
        user2.setUpdatedAt(java.time.Instant.now());

        UserProfile user2Profile = new UserProfile();
        user2Profile.setFirstName("Current");
        user2Profile.setLastName("User");
        user2Profile.setUser(user2);
        user2.setUserProfile(user2Profile);

        userRepository.saveAll(List.of(user1, user2));

        UpdateUserRequest updateUserRequest = UpdateUserRequest.builder()
                .email("taken@example.com")
                .build();

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user1.getUserId())
                .password("Password@123")
                .roles("USER")
                .build();

        mockMvc.perform(put("/users/profile")
                .with(SecurityMockMvcRequestPostProcessors.user(userDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateUserRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Email already exists")));

        User unchangedUser = userRepository.findById(user1.getUserId()).orElseThrow();
        assertThat(unchangedUser.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should delete current user successfully")
    void deleteCurrentUser_ShouldReturnNoContent() throws Exception {
        User user1 = new User();
        user1.setEmail("test@example.com");
        user1.setPassword(passwordEncoder.encode("Password@123"));
        user1.setIsActive(true);
        user1.setCreatedAt(java.time.Instant.now());
        user1.setUpdatedAt(java.time.Instant.now());

        UserProfile user1Profile = new UserProfile();
        user1Profile.setFirstName("Current");
        user1Profile.setLastName("User");
        user1Profile.setUser(user1);
        user1.setUserProfile(user1Profile);

        userRepository.saveAndFlush(user1);

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user1.getUserId())
                .password("Password@123")
                .roles("USER")
                .build();

        mockMvc.perform(delete("/users/profile")
                .with(SecurityMockMvcRequestPostProcessors.user(userDetails))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should change password successfully")
    void changePassword_ShouldReturnNoContent() throws Exception {
        User user1 = new User();
        user1.setEmail("test@example.com");
        user1.setPassword(passwordEncoder.encode("CurrentPassword@123"));
        user1.setIsActive(true);
        user1.setCreatedAt(java.time.Instant.now());
        user1.setUpdatedAt(java.time.Instant.now());

        UserProfile user1Profile = new UserProfile();
        user1Profile.setFirstName("Current");
        user1Profile.setLastName("User");
        user1Profile.setUser(user1);
        user1.setUserProfile(user1Profile);

        userRepository.saveAndFlush(user1);

        ChangePasswordRequest changePasswordRequest = ChangePasswordRequest.builder()
                .oldPassword("CurrentPassword@123")
                .newPassword("NewPassword@123")
                .build();

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user1.getUserId())
                .password("CurrentPassword@123")
                .roles("USER")
                .build();

        mockMvc.perform(post("/users/change-password")
                .with(SecurityMockMvcRequestPostProcessors.user(userDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Password changed successfully")));

        User updatedUser = userRepository.findById(user1.getUserId()).orElseThrow();
        assertThat(passwordEncoder.matches("NewPassword@123", updatedUser.getPassword())).isTrue();
    }

    @Test
    @DisplayName("Should return 401 when changing password with invalid current password")
    void changePassword_ShouldReturnUnauthorized_WhenCurrentPasswordInvalid() throws Exception {
        User user1 = new User();
        user1.setEmail("test@example.com");
        user1.setPassword(passwordEncoder.encode("CurrentPassword@123"));
        user1.setIsActive(true);
        user1.setCreatedAt(java.time.Instant.now());
        user1.setUpdatedAt(java.time.Instant.now());

        UserProfile user1Profile = new UserProfile();
        user1Profile.setFirstName("Current");
        user1Profile.setLastName("User");
        user1Profile.setUser(user1);
        user1.setUserProfile(user1Profile);

        userRepository.saveAndFlush(user1);

        ChangePasswordRequest changePasswordRequest = ChangePasswordRequest.builder()
                .oldPassword("WrongPassword@123")
                .newPassword("NewPassword@123")
                .build();

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user1.getUserId())
                .password("CurrentPassword@123")
                .roles("USER")
                .build();

        mockMvc.perform(post("/users/change-password")
                .with(SecurityMockMvcRequestPostProcessors.user(userDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("Provided password is incorrect")));

        User unchangedUser = userRepository.findById(user1.getUserId()).orElseThrow();
        assertThat(passwordEncoder.matches("CurrentPassword@123", unchangedUser.getPassword())).isTrue();
    }
}
