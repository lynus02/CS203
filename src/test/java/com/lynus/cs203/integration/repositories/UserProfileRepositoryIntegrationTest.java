package com.lynus.cs203.integration.repositories;

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
@DisplayName("User Profile Repository Integration Tests")
public class UserProfileRepositoryIntegrationTest {

    @Autowired
    private UserProfileRepository userProfileRepository;

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
        userProfileRepository.deleteAll();
        userRepository.deleteAll();

        testUser1 = createUser("johndoe@example.com", "Password@123");
        testUser2 = createUser("janedoe@example.com", "Password@123");
        testUserProfile1 = createUserProfile(testUser1, "John", "Doe");
        testUserProfile2 = createUserProfile(testUser2, "Jane", "Doe");

        testUserProfile1.setUser(testUser1);
        testUser1.setUserProfile(testUserProfile1);
        testUserProfile2.setUser(testUser2);
        testUser2.setUserProfile(testUserProfile2);

        userRepository.saveAllAndFlush(List.of(testUser1, testUser2));
    }

    private User createUser(String email, String password) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setIsActive(true);
        user.setCreatedAt(java.time.Instant.now());
        user.setUpdatedAt(java.time.Instant.now());
        return user;
    }

    private UserProfile createUserProfile(User user, String firstName, String lastName) {
        UserProfile profile = new UserProfile();
        profile.setFirstName(firstName);
        profile.setLastName(lastName);
        profile.setUser(user);
        return profile;
    }

    @Test
    @DisplayName("Should find user profile by ID with their user relationship")
    void whenFindById_ThenReturnUserProfileWithUser() {
        // When
        Optional<UserProfile> foundProfile = userProfileRepository.findById(testUserProfile1.getUserId());

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
        assertThat(profiles).hasSize(2);
        assertThat(profiles).extracting(UserProfile::getFirstName)
                .containsExactlyInAnyOrder("John", "Jane");
        assertThat(profiles).extracting(p -> p.getUser().getEmail())
                .containsExactlyInAnyOrder("johndoe@example.com", "janedoe@example.com");
    }

    @Test
    @DisplayName("Should save new user profile with user relationship")
    void whenSave_ThenPersistUserProfileWithUser() {
        // Given
        User newUser = new User();
        newUser.setEmail("newuser@example.com");
        newUser.setPassword(passwordEncoder.encode("Password@123"));
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
        Optional<UserProfile> foundProfile = userProfileRepository.findById(newProfile.getUserId());
        assertThat(foundProfile).isPresent();
        assertThat(foundProfile.get().getFirstName()).isEqualTo("New");
        assertThat(foundProfile.get().getLastName()).isEqualTo("User");
        assertThat(foundProfile.get().getUser().getEmail()).isEqualTo("newuser@example.com");

    }

    @Test
    @DisplayName("Should update existing user profile")
    void whenUpdateUserProfile_ThenUserProfileIsUpdated() {
        // Given
        testUserProfile1.setFirstName("UpdatedFirstName");
        testUserProfile1.setLastName("UpdatedLastName");

        // When
        UserProfile updatedProfile = userProfileRepository.saveAndFlush(testUserProfile1);

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
        // When
        userProfileRepository.delete(testUserProfile1);

        // Then
        Optional<UserProfile> foundProfile = userProfileRepository.findById(testUserProfile1.getUserId());
        assertThat(foundProfile).isEmpty();

        // Verify
        Optional<User> foundUser = userRepository.findById(testUser1.getUserId());
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("johndoe@example.com");
    }

    @Test
    @DisplayName("Should delete user and cascade delete user profile")
    void whenDeleteUser_ThenUserProfileIsAlsoDeleted() {
        // When
        userRepository.delete(testUser1);

        // Then
        Optional<User> foundUser = userRepository.findById(testUser1.getUserId());
        assertThat(foundUser).isEmpty();

        Optional<UserProfile> foundProfile = userProfileRepository.findById(testUserProfile1.getUserId());
        assertThat(foundProfile).isEmpty();
    }

    @Test
    @DisplayName("Should find user profile by email")
    void whenFindByEmail_ThenReturnUserProfile() {
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
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should return true when checking existence by ID")
    void whenExistsById_ThenReturnTrueIfExists() {
        // When
        boolean exists = userProfileRepository.existsById(testUserProfile1.getUserId());

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
        User newUser = new User();
        newUser.setEmail("newuser@example.com");
        newUser.setPassword(passwordEncoder.encode("Password@123"));
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
        assertThat(savedUser.getUserProfile()).isNotNull();
        assertThat(savedUser.getUserProfile().getUser()).isEqualTo(newUser);

        Optional<UserProfile> foundProfile = userProfileRepository.findById(newProfile.getUserId());
        assertThat(foundProfile).isPresent();
        assertThat(foundProfile.get().getUser()).isEqualTo(savedUser);
    }
}
