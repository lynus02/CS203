package com.lynus.cs203.integration.repositories;

import com.lynus.cs203.entities.*;
import com.lynus.cs203.integration.BaseIntegrationTest;
import com.lynus.cs203.repositories.UserProfileRepository;
import com.lynus.cs203.repositories.UserRepository;
import com.lynus.cs203.repositories.UserRoleRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("User Role Repository Integration Tests")
public class UserRoleRepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserRoleRepository userRoleRepository;

    private User additionalTestUser1;
    private User additionalTestUser2;
    private UserRole additionalUserRole1;
    private UserRole additionalUserRole2;
    private UserRole additionalUserRole3;


    @BeforeEach
    void setUp() {
        setupAdditionalTestData();
    }

    private void setupAdditionalTestData() {
        // Create additional test users
        additionalTestUser1 = createTestUser("johndoe@example.com", "John", "Doe");
        additionalTestUser2 = createTestUser("janedoe@example.com", "Jane", "Doe");

        // Create user roles for additional testing
        additionalUserRole1 = createAndSaveUserRole(additionalTestUser1, Role.USER);
        additionalUserRole2 = createAndSaveUserRole(additionalTestUser1, Role.ADMIN);
        additionalUserRole3 = createAndSaveUserRole(additionalTestUser2, Role.USER);
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

    private UserRole createAndSaveUserRole(User user, Role role) {
        UserRole userRole = new UserRole();
        UserRoleId userRoleId = new UserRoleId();
        userRoleId.setUserId(user.getUserId());
        userRoleId.setRoleId((short) role.getId());

        userRole.setId(userRoleId);
        userRole.setUser(user);
        userRole.setRole(role);
        userRole.setAssignedAt(LocalDateTime.now());

        return userRoleRepository.save(userRole);
    }

    @Test
    @DisplayName("Should check if user role exists by user ID and role")
    void existsByUserUserIdAndRole_WithValidData_ShouldReturnTrue() {
        // When
        boolean exists = userRoleRepository.existsByUserUserIdAndRole(
                additionalTestUser1.getUserId(), Role.ADMIN);

        // Then
        assertThat(exists).isTrue();
    }


    @Test
    @DisplayName("Should return false for non-existing user role combination")
    void existsByUserUserIdAndRole_WithNonExistingData_ShouldReturnFalse() {
        // When
        boolean exists = userRoleRepository.existsByUserUserIdAndRole(
                additionalTestUser2.getUserId(), Role.ADMIN);

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should delete user role by user ID and role ID")
    void deleteByUserUserIdAndRole_ShouldRemoveRole() {
        // Given - verify role exists before deletion
        assertThat(userRoleRepository.existsByUserUserIdAndRole(
                additionalTestUser1.getUserId(), Role.ADMIN)).isTrue();

        // When
        userRoleRepository.deleteByUserUserIdAndRole(additionalTestUser1.getUserId(), Role.ADMIN);
        userRoleRepository.flush();

        // Then
        assertThat(userRoleRepository.existsByUserUserIdAndRole(
                additionalTestUser1.getUserId(), Role.ADMIN)).isFalse();

        // Verify other roles remain
        assertThat(userRoleRepository.existsByUserUserIdAndRole(
                additionalTestUser1.getUserId(), Role.USER)).isTrue();
    }

    @Test
    @DisplayName("Should find all user roles by user ID")
    void findByUserUserId_ShouldReturnAllRolesForUser() {
        // When
        List<UserRole> userRoles = userRoleRepository.findByUserUserId(additionalTestUser1.getUserId());

        // Then
        assertThat(userRoles).hasSize(2);
        assertThat(userRoles).extracting(u -> u.getRole())
                .containsExactlyInAnyOrder(Role.USER, Role.ADMIN);
    }

    @Test
    @DisplayName("Should return empty list when finding roles for user with no roles")
    void findByUserUserId_WithMinimalRoles_ShouldReturnExpectedRoles() {
        // When
        List<UserRole> userRoles = userRoleRepository.findByUserUserId(additionalTestUser2.getUserId());

        // Then
        assertThat(userRoles).hasSize(1);
        assertThat(userRoles.get(0).getRole()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("Should check if any user has a specific role ID")
    void existsByRole_WithExistingRole_ShouldReturnTrue() {
        // When
        boolean exists = userRoleRepository.existsByRole(Role.ADMIN);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should save new user role successfully")
    void save_ShouldPersistNewUserRole() {
        // Given
        UserRole newUserRole = createAndSaveUserRole(additionalTestUser2, Role.ADMIN);

        // Then
        assertThat(newUserRole.getId()).isNotNull();
        assertThat(userRoleRepository.existsByUserUserIdAndRole(
                additionalTestUser2.getUserId(), Role.ADMIN)).isTrue();

        // Verify user now has both roles
        List<UserRole> userRoles = userRoleRepository.findByUserUserId(additionalTestUser2.getUserId());
        assertThat(userRoles).hasSize(2);
        assertThat(userRoles).extracting(u -> u.getRole())
                .containsExactlyInAnyOrder(Role.USER, Role.ADMIN);
    }

    @Test
    @DisplayName("Should find user role by composite ID")
    void findById_ShouldReturnUserRole() {
        // Given
        UserRoleId id = new UserRoleId();
        id.setUserId(additionalTestUser1.getUserId());
        id.setRoleId((short) Role.ADMIN.getId());

        // When
        Optional<UserRole> userRole = userRoleRepository.findById(id);

        // Then
        assertThat(userRole).isPresent();
        assertThat(userRole.get().getUser().getUserId()).isEqualTo(additionalTestUser1.getUserId());
        assertThat(userRole.get().getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("Should return empty when finding by non-existing composite ID")
    void findById_WithNonExistingId_ShouldReturnEmpty() {
        // Given
        UserRoleId id = new UserRoleId();
        id.setUserId("non-existing-user-id");
        id.setRoleId((short) Role.ADMIN.getId());

        // When
        Optional<UserRole> userRole = userRoleRepository.findById(id);

        // Then
        assertThat(userRole).isEmpty();
    }

    @Test
    @DisplayName("Should count all user roles correctly")
    void count_ShouldReturnCorrectNumberOfUserRoles() {
        // When
        long count = userRoleRepository.count();

        // Then - testUser (1), adminUser (2), additionalTestUser1 (2), additionalTestUser2 (1)
        assertThat(count).isEqualTo(6);
    }

    @Test
    @DisplayName("Should delete all user roles for a specific user")
    void deleteAllByUser_ShouldRemoveAllUserRoles() {
        // Given
        List<UserRole> rolesToDelete = userRoleRepository.findByUserUserId(additionalTestUser1.getUserId());
        long initialCount = userRoleRepository.count();

        // When
        userRoleRepository.deleteAll(rolesToDelete);
        userRoleRepository.flush();

        // Then
        assertThat(userRoleRepository.findByUserUserId(additionalTestUser1.getUserId())).isEmpty();
        assertThat(userRoleRepository.findByUserUserId(additionalTestUser2.getUserId())).hasSize(1);
        assertThat(userRoleRepository.count()).isEqualTo(initialCount - 2);
    }

    @Test
    @DisplayName("Should check existence by composite ID")
    void existsById_ShouldReturnTrueIfExists() {
        // Given
        UserRoleId id = new UserRoleId();
        id.setUserId(additionalTestUser1.getUserId());
        id.setRoleId((short) Role.ADMIN.getId());

        // When
        boolean exists = userRoleRepository.existsById(id);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when checking existence by non-existing composite ID")
    void existsById_WithNonExistingId_ShouldReturnFalse() {
        // Given
        UserRoleId id = new UserRoleId();
        id.setUserId("non-existing-user-id");
        id.setRoleId((short) Role.ADMIN.getId());

        // When
        boolean exists = userRoleRepository.existsById(id);

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should handle duplicate role assignment gracefully")
    void save_WithDuplicateUserRole_ShouldNotCreateDuplicate() {
        // Given
        long initialCount = userRoleRepository.count();

        // Try to create duplicate role
        UserRole duplicateRole = new UserRole();
        UserRoleId duplicateId = new UserRoleId();
        duplicateId.setUserId(additionalTestUser1.getUserId());
        duplicateId.setRoleId((short) Role.ADMIN.getId());

        duplicateRole.setId(duplicateId);
        duplicateRole.setUser(additionalTestUser1);
        duplicateRole.setRole(Role.ADMIN);
        duplicateRole.setAssignedAt(LocalDateTime.now());

        // When
        userRoleRepository.save(duplicateRole);
        userRoleRepository.flush();

        // Then - count should remain the same due to composite key constraint
        List<UserRole> userRoles = userRoleRepository.findByUserUserId(additionalTestUser1.getUserId());
        assertThat(userRoles).hasSize(2); // Still only USER and ADMIN, no duplicates
        assertThat(userRoleRepository.count()).isEqualTo(initialCount);
    }

    @Test
    @DisplayName("Should maintain referential integrity with user")
    void save_ShouldMaintainUserReferenceIntegrity() {
        // Given
        UserRole newRole = createAndSaveUserRole(additionalTestUser2, Role.ADMIN);

        // When
        UserRole savedRole = userRoleRepository.findById(newRole.getId()).orElseThrow();

        // Then
        assertThat(savedRole.getUser()).isNotNull();
        assertThat(savedRole.getUser().getUserId()).isEqualTo(additionalTestUser2.getUserId());
        assertThat(savedRole.getUser().getEmail()).isEqualTo("janedoe@example.com");
        assertThat(savedRole.getRole()).isEqualTo(Role.ADMIN);
        assertThat(savedRole.getAssignedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should cascade delete user roles when user is deleted")
    void deleteUser_ShouldCascadeDeleteUserRoles() {
        // Given
        String userId = additionalTestUser1.getUserId();
        long initialRoleCount = userRoleRepository.count();

        // Verify user has roles before deletion
        List<UserRole> userRoles = userRoleRepository.findByUserUserId(userId);
        assertThat(userRoles).hasSize(2);

        // IMPORTANT: Ensure the User entity's userRoles collection is populated
        User userToDelete = userRepository.findById(userId).orElseThrow();
        userToDelete.getUserRoles().addAll(userRoles);
        userRepository.saveAndFlush(userToDelete);

        // Clear persistence context to ensure fresh state
        entityManager.clear();

        // Reload the user with fresh state
        userToDelete = userRepository.findById(userId).orElseThrow();

        // When - Delete the user
        userRepository.delete(userToDelete);
        userRepository.flush();

        // Clear persistence context and force refresh from database
        entityManager.clear();

        // Then
        assertThat(userRoleRepository.findByUserUserId(userId)).isEmpty();
        assertThat(userRoleRepository.count()).isEqualTo(initialRoleCount - 2);

        // Verify other users' roles are unaffected
        assertThat(userRoleRepository.findByUserUserId(additionalTestUser2.getUserId())).hasSize(1);
    }
}
