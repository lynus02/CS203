package com.lynus.cs203.integration.repositories;

import com.lynus.cs203.Cs203Application;
import com.lynus.cs203.entities.User;
import com.lynus.cs203.entities.UserProfile;
import com.lynus.cs203.integration.BaseIntegrationTest;
import com.lynus.cs203.repositories.UserProfileRepository;
import com.lynus.cs203.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("User Profile Repository Integration Tests")
public class UserProfileRepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserProfileRepository userProfileRepository;

    private User additionalTestUser1;
    private User additionalTestUser2;
    private UserProfile additionalTestProfile1;
    private UserProfile additionalTestProfile2;

    @BeforeEach
    void setUp() {
        setupAdditionalTestData();
    }

    private void setupAdditionalTestData() {
        // Create additional test users for profile testing
        additionalTestUser1 = createTestUser("johndoe@example.com", "John", "Doe");
        additionalTestUser2 = createTestUser("janedoe@example.com", "Jane", "Doe");

        additionalTestProfile1 = additionalTestUser1.getUserProfile();
        additionalTestProfile2 = additionalTestUser2.getUserProfile();
    }

    private User createTestUser(String email, String firstName, String lastName) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("Password@123!"));
        user.setIsActive(true);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());

        UserProfile profile = new UserProfile();
        profile.setFirstName(firstName);
        profile.setLastName(lastName);
        profile.setUser(user);
        user.setUserProfile(profile);

        return userRepository.save(user);
    }

    @Test
    @DisplayName("Should find user profile by ID with their user relationship")
    void whenFindById_ThenReturnUserProfileWithUser() {
        // When
        Optional<UserProfile> foundProfile = userProfileRepository.findById(additionalTestProfile1.getUserId());

        // Then
        assertThat(foundProfile).isPresent();
        assertThat(foundProfile.get().getFirstName()).isEqualTo("John");
        assertThat(foundProfile.get().getLastName()).isEqualTo("Doe");
        assertThat(foundProfile.get().getUser()).isNotNull();
        assertThat(foundProfile.get().getUser().getEmail()).isEqualTo("johndoe@example.com");
        assertThat(foundProfile.get().getUser().getIsActive()).isTrue();
    }

    @Test
    @DisplayName("Should find all user profiles with their user relationships")
    void whenFindAll_ThenReturnAllUserProfilesWithUsers() {
        // When
        List<UserProfile> profiles = userProfileRepository.findAll();

        // Then
        assertThat(profiles).hasSize(4); // testUser, adminUser, additionalTestUser1, additionalTestUser2

        // Verify all profiles have users
        profiles.forEach(profile -> {
            assertThat(profile.getUser()).isNotNull();
            assertThat(profile.getUser().getEmail()).isNotNull();
            assertThat(profile.getFirstName()).isNotNull();
            assertThat(profile.getLastName()).isNotNull();
        });
    }

    @Test
    @DisplayName("Should save new user profile with user relationship")
    void whenSave_ThenPersistUserProfileWithUser() {
        // Given
        User newUser = createTestUser("newuser@example.com", "New", "User");

        // Then
        Optional<UserProfile> foundProfile = userProfileRepository.findById(newUser.getUserProfile().getUserId());
        assertThat(foundProfile).isPresent();
        assertThat(foundProfile.get().getFirstName()).isEqualTo("New");
        assertThat(foundProfile.get().getLastName()).isEqualTo("User");
        assertThat(foundProfile.get().getUser().getEmail()).isEqualTo("newuser@example.com");
    }

    @Test
    @DisplayName("Should update existing user profile")
    void whenUpdateUserProfile_ThenUserProfileIsUpdated() {
        // Given
        additionalTestProfile1.setFirstName("UpdatedFirstName");
        additionalTestProfile1.setLastName("UpdatedLastName");

        // When
        UserProfile updatedProfile = userProfileRepository.saveAndFlush(additionalTestProfile1);

        // Then
        Optional<UserProfile> foundProfile = userProfileRepository.findById(updatedProfile.getUserId());
        assertThat(foundProfile).isPresent();
        assertThat(foundProfile.get().getFirstName()).isEqualTo("UpdatedFirstName");
        assertThat(foundProfile.get().getLastName()).isEqualTo("UpdatedLastName");
        assertThat(foundProfile.get().getUser().getEmail()).isEqualTo("johndoe@example.com");
    }

    @Test
    @DisplayName("Should delete user profile while keeping user intact")
    void whenDeleteUserProfile_ThenUserRemains() {
        // Given - Get the user ID before deletion
        String userId = additionalTestUser1.getUserId();
        String userProfileId = additionalTestProfile1.getUserId();

        // First, break the bidirectional relationship
        additionalTestUser1.setUserProfile(null);
        userRepository.saveAndFlush(additionalTestUser1);

        // When - Delete the profile
        userProfileRepository.delete(additionalTestProfile1);
        userProfileRepository.flush();

        // Then - Profile should be deleted
        Optional<UserProfile> foundProfile = userProfileRepository.findById(userProfileId);
        assertThat(foundProfile).isEmpty();

        // Verify user still exists without profile
        Optional<User> foundUser = userRepository.findById(userId);
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("johndoe@example.com");
        assertThat(foundUser.get().getUserProfile()).isNull();
    }

    @Test
    @DisplayName("Should delete user and cascade delete user profile")
    void whenDeleteUser_ThenUserProfileIsAlsoDeleted() {
        // When
        userRepository.delete(additionalTestUser1);
        userRepository.flush();

        // Then
        Optional<User> foundUser = userRepository.findById(additionalTestUser1.getUserId());
        assertThat(foundUser).isEmpty();

        Optional<UserProfile> foundProfile = userProfileRepository.findById(additionalTestProfile1.getUserId());
        assertThat(foundProfile).isEmpty();
    }

    @Test
    @DisplayName("Should find user profile by email")
    void whenFindByEmail_ThenReturnUserProfile() {
        // When
        Optional<UserProfile> foundProfile = userProfileRepository.findAll().stream()
                .filter(p -> p.getUser().getEmail().equals("johndoe@example.com"))
                .findFirst();

        // Then
        assertThat(foundProfile).isPresent();
        assertThat(foundProfile.get().getFirstName()).isEqualTo("John");
        assertThat(foundProfile.get().getLastName()).isEqualTo("Doe");
    }

    @Test
    @DisplayName("Should return correct count of user profiles")
    void whenCount_ThenReturnNumberOfUserProfiles() {
        // When
        long count = userProfileRepository.count();

        // Then
        assertThat(count).isEqualTo(4); // testUser, adminUser, additionalTestUser1, additionalTestUser2
    }

    @Test
    @DisplayName("Should return true when checking existence by ID")
    void whenExistsById_ThenReturnTrueIfExists() {
        // When
        boolean exists = userProfileRepository.existsById(additionalTestProfile1.getUserId());

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when checking existence by non-existing ID")
    void whenExistsById_ThenReturnFalseIfNotExists() {
        // When
        boolean exists = userProfileRepository.existsById("non-existing-id");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should maintain bidirectional relationship integrity")
    void whenSavingUserWithProfile_ThenBothSidesAreConsistent() {
        // Given
        User newUser = createTestUser("integrity@example.com", "Integrity", "Test");

        // Then
        assertThat(newUser.getUserProfile()).isNotNull();
        assertThat(newUser.getUserProfile().getUser()).isEqualTo(newUser);

        Optional<UserProfile> foundProfile = userProfileRepository.findById(newUser.getUserProfile().getUserId());
        assertThat(foundProfile).isPresent();
        assertThat(foundProfile.get().getUser()).isEqualTo(newUser);
        assertThat(foundProfile.get().getUser().getUserProfile()).isEqualTo(foundProfile.get());
    }

    @Test
    @DisplayName("Should handle profile updates without affecting user")
    void updateProfile_ShouldNotAffectUserData() {
        // Given
        String originalEmail = additionalTestUser1.getEmail();
        boolean originalActiveStatus = additionalTestUser1.getIsActive();

        // When
        additionalTestProfile1.setFirstName("Modified");
        additionalTestProfile1.setLastName("Name");
        userProfileRepository.saveAndFlush(additionalTestProfile1);

        // Then
        User refreshedUser = userRepository.findById(additionalTestUser1.getUserId()).orElseThrow();
        assertThat(refreshedUser.getEmail()).isEqualTo(originalEmail);
        assertThat(refreshedUser.getIsActive()).isEqualTo(originalActiveStatus);
        assertThat(refreshedUser.getUserProfile().getFirstName()).isEqualTo("Modified");
        assertThat(refreshedUser.getUserProfile().getLastName()).isEqualTo("Name");
    }
}
