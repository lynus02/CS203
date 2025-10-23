package com.lynus.cs203.integration.repositories;

import com.lynus.cs203.entities.User;
import com.lynus.cs203.entities.UserProfile;
import com.lynus.cs203.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:mysql://localhost:3306/tariff",
        "spring.datasource.username=root",
        "spring.datasource.password=password123"
})
@Transactional
@DisplayName("User Repository Integration Tests")
public class UserRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser1;
    private User testUser2;
    private UserProfile testUserProfile1;
    private UserProfile testUserProfile2;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        // First test user
        testUser1 = new User();
        testUser1.setEmail("johndoe@example.com");
        testUser1.setPassword(passwordEncoder.encode("Password@123"));
        testUser1.setIsActive(true);
        testUser1.setCreatedAt(java.time.Instant.now());
        testUser1.setUpdatedAt(java.time.Instant.now());

        testUserProfile1 = new UserProfile();
        testUserProfile1.setFirstName("John");
        testUserProfile1.setLastName("Doe");
        testUserProfile1.setUser(testUser1);

        testUser1.setUserProfile(testUserProfile1);

        // Second test user
        testUser2 = new User();
        testUser2.setEmail("janedoe@example.com");
        testUser2.setPassword(passwordEncoder.encode("Password@123"));
        testUser2.setIsActive(true);
        testUser2.setCreatedAt(java.time.Instant.now());
        testUser2.setUpdatedAt(java.time.Instant.now());

        testUserProfile2 = new UserProfile();
        testUserProfile2.setFirstName("Jane");
        testUserProfile2.setLastName("Doe");
        testUserProfile2.setUser(testUser2);

        testUser2.setUserProfile(testUserProfile2);

        userRepository.saveAllAndFlush(List.of(testUser1, testUser2));
    }

    @Test
    @DisplayName("Should save user with profile")
    void save_ShouldPersistUserWithProfile() {
        // Given
        User newUser = new User();
        newUser.setEmail("newuser@example.com");
        newUser.setPassword(passwordEncoder.encode("Password@123!"));
        newUser.setIsActive(true);
        newUser.setCreatedAt(java.time.Instant.now());
        newUser.setUpdatedAt(java.time.Instant.now());

        UserProfile newProfile = new UserProfile();
        newProfile.setFirstName("New");
        newProfile.setLastName("User");
        newProfile.setUser(newUser);
        newUser.setUserProfile(newProfile);

        // When
        User savedUser = userRepository.saveAndFlush(newUser);

        // Then
        assertThat(savedUser.getUserId()).isNotNull();
        assertThat(savedUser.getEmail()).isEqualTo("newuser@example.com");
        assertThat(savedUser.getUserProfile()).isNotNull();
        assertThat(savedUser.getUserProfile().getFirstName()).isEqualTo("New");

        // Verify it can be retrieved from database
        Optional<User> retrievedUser = userRepository.findByEmail("newuser@example.com");
        assertThat(retrievedUser).isPresent();
        assertThat(retrievedUser.get().getUserProfile().getFirstName()).isEqualTo("New");
    }

    @Test
    @DisplayName("Should update user profile")
    void update_ShouldUpdateUserProfile() {
        // Given
        User existingUser = userRepository.findByEmail("johndoe@example.com").orElseThrow();
        existingUser.getUserProfile().setFirstName("Updated");
        existingUser.getUserProfile().setLastName("Name");

        // When
        User updatedUser = userRepository.saveAndFlush(existingUser);

        // Then
        assertThat(updatedUser.getUserProfile().getFirstName()).isEqualTo("Updated");
        assertThat(updatedUser.getUserProfile().getLastName()).isEqualTo("Name");

        // Verify changes
        User retrievedUser = userRepository.findByEmail("johndoe@example.com").orElseThrow();
        assertThat(retrievedUser.getUserProfile().getFirstName()).isEqualTo("Updated");
        assertThat(retrievedUser.getUserProfile().getLastName()).isEqualTo("Name");
    }

    @Test
    @DisplayName("Should delete user")
    void delete_ShouldRemoveUser() {
        // Given
        User userToDelete = userRepository.findByEmail("johndoe@example.com").orElseThrow();

        // When
        userRepository.delete(userToDelete);

        // Then
        assertThat(userRepository.findByEmail("johndoe@example.com")).isEmpty();
        assertThat(userRepository.existsByEmail("johndoe@example.com")).isFalse();
    }

    @Test
    @DisplayName("Should find user by email")
    void findByEmail_ShouldReturnUser_WhenEmailExists() {
        // When
        Optional<User> foundUser = userRepository.findByEmail("johndoe@example.com");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("johndoe@example.com");
        assertThat(foundUser.get().getIsActive()).isTrue();
        assertThat(foundUser.get().getUserProfile()).isNotNull();
        assertThat(foundUser.get().getUserProfile().getFirstName()).isEqualTo("John");
        assertThat(foundUser.get().getUserProfile().getLastName()).isEqualTo("Doe");
    }

    @Test
    @DisplayName("Should return empty when email does not exist")
    void findByEmail_ShouldReturnEmpty_WhenEmailDoesNotExist() {
        // When
        Optional<User> foundUser = userRepository.findByEmail("nonexistent@example.com");

        // Then
        assertThat(foundUser).isEmpty();
    }

    @Test
    @DisplayName("Should find user by email with profile")
    void findByEmailWithProfile_ShouldReturnUserWithProfile_WhenEmailExists() {
        // When
        Optional<User> foundUser = userRepository.findByEmailWithProfile("johndoe@example.com");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("johndoe@example.com");
        assertThat(foundUser.get().getUserProfile()).isNotNull();
        assertThat(foundUser.get().getUserProfile().getFirstName()).isEqualTo("John");
        assertThat(foundUser.get().getUserProfile().getLastName()).isEqualTo("Doe");
    }

    @Test
    @DisplayName("Should return empty when finding by email with profile if email does not exist")
    void findByEmailWithProfile_ShouldReturnEmpty_WhenEmailDoesNotExist() {
        // When
        Optional<User> foundUser = userRepository.findByEmailWithProfile("unknown@example.com");

        // Then
        assertThat(foundUser).isEmpty();
    }

    @Test
    @DisplayName("Should find user by ID with profile")
    void findById_ShouldReturnUserWithProfile_WhenIdExists() {
        // Given
        User existingUser = userRepository.findByEmail("johndoe@example.com").orElseThrow();
        String userId = existingUser.getUserId();

        // When
        Optional<User> foundUser = userRepository.findById(userId);

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUserId()).isEqualTo(userId);
        assertThat(foundUser.get().getEmail()).isEqualTo("johndoe@example.com");
        assertThat(foundUser.get().getUserProfile()).isNotNull();
        assertThat(foundUser.get().getUserProfile().getFirstName()).isEqualTo("John");
        assertThat(foundUser.get().getUserProfile().getLastName()).isEqualTo("Doe");
    }

    @Test
    @DisplayName("Should return empty when finding by ID with profile if ID does not exist")
    void findById_ShouldReturnEmpty_WhenIdDoesNotExist() {
        // When
        Optional<User> foundUser = userRepository.findById("nonexistent-id");

        // Then
        assertThat(foundUser).isEmpty();
    }

    @Test
    @DisplayName("Should find all users with profiles")
    void findAll_ShouldReturnAllUsersWithProfiles() {
        // When
        List<User> users = userRepository.findAll();

        // Then
        assertThat(users).hasSize(2);

        // Verify
        users.forEach(user -> {
            assertThat(user.getUserProfile()).isNotNull();
            assertThat(user.getUserProfile().getFirstName()).isNotNull();
            assertThat(user.getUserProfile().getLastName()).isNotNull();
        });
    }

    @Test
    @DisplayName("Should check if email exists")
    void existsByEmail_ShouldReturnTrue_WhenEmailExists() {
        // When
        boolean exists = userRepository.existsByEmail("johndoe@example.com");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false if email does not exist")
    void existsByEmail_ShouldReturnFalse_WhenEmailDoesNotExist() {
        // When
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should find only active users")
    void findByEmail_ShouldReturnOnlyActiveUsers() {
        // Given
        User inactiveUser = new User();
        inactiveUser.setEmail("inactive@example.com");
        inactiveUser.setPassword(passwordEncoder.encode("Password@123"));
        inactiveUser.setIsActive(false);
        inactiveUser.setCreatedAt(java.time.Instant.now());
        inactiveUser.setUpdatedAt(java.time.Instant.now());

        UserProfile profile = new UserProfile();
        profile.setFirstName("Inactive");
        profile.setLastName("User");
        profile.setUser(inactiveUser);
        inactiveUser.setUserProfile(profile);

        userRepository.saveAndFlush(inactiveUser);

        // When
        Optional<User> foundInactiveUser = userRepository.findByEmail("inactive@example.com");

        // Then
        assertThat(foundInactiveUser).isPresent();
        assertThat(foundInactiveUser.get().getIsActive()).isFalse();

        // Verify
        Optional<User> foundActiveUser = userRepository.findByEmail("johndoe@example.com");
        assertThat(foundActiveUser).isPresent();
        assertThat(foundActiveUser.get().getIsActive()).isTrue();
    }

    @Test
    @DisplayName("Should cascade delete user profile when user is deleted")
    void delete_ShouldCascadeDeleteUserProfile() {
        // Given
        User userToDelete = userRepository.findByEmail("johndoe@example.com").orElseThrow();
        String userId = userToDelete.getUserId();

        // When
        userRepository.delete(userToDelete);

        // Then
        Optional<User> deletedUser = userRepository.findById(userId);
        assertThat(deletedUser).isEmpty();

        // Verify
        long userCount = userRepository.count();
        assertThat(userCount).isEqualTo(1); // Only Jane Doe should remain
    }
}
