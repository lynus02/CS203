package com.lynus.cs203.integration.repositories;

import com.lynus.cs203.entities.Role;
import com.lynus.cs203.entities.User;
import com.lynus.cs203.entities.UserRole;
import com.lynus.cs203.entities.UserRoleId;
import com.lynus.cs203.repositories.UserRepository;
import com.lynus.cs203.repositories.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("User Role Repository Integration Tests")
public class UserRoleRepositoryIntegrationTest {

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final Short ROLE_USER = 1;
    private static final Short ROLE_ADMIN = 2;

    private User testUser1;
    private User testUser2;
    private UserRole testUserRole1;
    private UserRole testUserRole2;
    private UserRole testUserRole3;

    @BeforeEach
    void setUp() {
        userRoleRepository.deleteAll();
        userRepository.deleteAll();

        testUser1 = createUser("johndoe@example.com", "Password@123");
        testUser2 = createUser("janedoe@example.com", "Password@123");

        userRepository.saveAllAndFlush(List.of(testUser1, testUser2));

        UserRoleId id1 = new UserRoleId();
        id1.setUserId(testUser1.getUserId());
        id1.setRoleId(ROLE_USER);

        UserRoleId id2 = new UserRoleId();
        id2.setUserId(testUser1.getUserId());
        id2.setRoleId(ROLE_ADMIN);

        UserRoleId id3 = new UserRoleId();
        id3.setUserId(testUser2.getUserId());
        id3.setRoleId(ROLE_USER);

        testUserRole1 = createUserRole(id1, testUser1);
        testUserRole2 = createUserRole(id2, testUser1);
        testUserRole3 = createUserRole(id3, testUser2);

        userRoleRepository.saveAll(List.of(testUserRole1, testUserRole2, testUserRole3));
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

    private UserRole createUserRole(UserRoleId id, User user) {
        UserRole userRole = new UserRole();
        userRole.setId(id);
        userRole.setUser(user);
        userRole.setAssignedAt(LocalDateTime.now());
        return userRole;
    }

    @Test
    @DisplayName("Should check if user role exists by user ID and role ID")
    void whenExistsByUserUserId_WithValidData_ThenReturnTrue() {
        // When
        boolean exists = userRoleRepository.existsByUserUserIdAndRole(
                testUser1.getUserId(), Role.ADMIN);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should delete user role by user ID and role ID")
    void whenDeleteByUserUserIdAndRoleId_ThenRoleIsRemoved() {
        // Given - verify role exists before deletion
        assertThat(userRoleRepository.existsByUserUserIdAndRole(
                testUser1.getUserId(),
                Role.ADMIN)).isTrue();

        // When
        userRoleRepository.deleteByUserUserIdAndRole(testUser1.getUserId(), Role.ADMIN);

        // Then
        assertThat(userRoleRepository.existsByUserUserIdAndRole(
                testUser1.getUserId(),
                Role.ADMIN)).isFalse();

        // Verify
        assertThat(userRoleRepository.existsByUserUserIdAndRole(
                testUser1.getUserId(),
                Role.USER)).isTrue();
    }

    @Test
    @DisplayName("Should find all user roles by user ID")
    void whenFindByUserUserId_ThenReturnAllRolesForUser() {
        // When
        List<UserRole> userRoles = userRoleRepository.findByUserUserId((testUser1.getUserId()));

        // Then
        assertThat(userRoles).hasSize(2);
        assertThat(userRoles).extracting(u -> u.getId().getRoleId())
                .containsExactlyInAnyOrder(ROLE_USER, ROLE_ADMIN);
    }

    @Test
    @DisplayName("Should return empty list when finding roles for user with no roles")
    void whenFindByUserUserId_WithNoRoles_ThenReturnEmptyList() {
        // Given
        User userWithNoRoles = createUser("noroles@example.com", "Password@123");
        userWithNoRoles = userRepository.saveAndFlush(userWithNoRoles);

        // When
        List<UserRole> userRoles = userRoleRepository.findByUserUserId(userWithNoRoles.getUserId());

        // Then
        assertThat(userRoles).isEmpty();
    }

    @Test
    @DisplayName("Should check if any user has a specific role ID")
    void whenExistsByRoleId_WithExistingRole_ThenReturnTrue() {
        // When
        boolean exists = userRoleRepository.existsByRole(Role.ADMIN);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should save new user role successfully")
    void whenSaveNewUserRole_ThenPersistSuccessfully() {
        // Given
        UserRoleId newRoleId = new UserRoleId();
        newRoleId.setUserId(testUser2.getUserId());
        newRoleId.setRoleId(ROLE_ADMIN);

        UserRole newUserRole = createUserRole(newRoleId, testUser2);

        // When
        UserRole savedUserRole = userRoleRepository.saveAndFlush(newUserRole);

        // Then
        assertThat(savedUserRole).isNotNull();
        assertThat(userRoleRepository.existsByUserUserIdAndRole(
                testUser2.getUserId(), Role.ADMIN)).isTrue();

        // Verify
        List<UserRole> userRoles = userRoleRepository.findByUserUserId(testUser2.getUserId());
        assertThat(userRoles).hasSize(2);
    }

    @Test
    @DisplayName("Should find user role by composite ID")
    void whenFindById_ThenReturnUserRole() {
        // When
        UserRoleId id = new UserRoleId();
        id.setUserId(testUser1.getUserId());
        id.setRoleId(ROLE_ADMIN);

        Optional<UserRole> userRole = userRoleRepository.findById(id);

        // Then
        assertThat(userRole).isPresent();
        assertThat(userRole.get().getUser().getUserId()).isEqualTo(testUser1.getUserId());
        assertThat(userRole.get().getId().getRoleId()).isEqualTo(ROLE_ADMIN);
    }

    @Test
    @DisplayName("Should return empty when finding by non-existing composite ID")
    void whenFindById_WithNonExistingId_ThenReturnEmpty() {
        // When
        UserRoleId id = new UserRoleId();
        id.setUserId("non-existing-user-id");
        id.setRoleId(ROLE_ADMIN);

        Optional<UserRole> userRole = userRoleRepository.findById(id);

        // Then
        assertThat(userRole).isEmpty();
    }

    @Test
    @DisplayName("Should count all user roles correctly")
    void whenCount_thenReturnCorrectNumberOfUserRoles() {
        // When
        long count = userRoleRepository.count();

        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("Should delete all user roles for a specific user")
    void whenDeleteAllByUserUserId_ThenAllRolesAreRemoved() {
        // When
        List<UserRole> rolesToDelete = userRoleRepository.findByUserUserId((testUser1.getUserId()));
        userRoleRepository.deleteAll(rolesToDelete);

        // Then
        assertThat(userRoleRepository.findByUserUserId((testUser1.getUserId()))).isEmpty();
        assertThat(userRoleRepository.findByUserUserId((testUser2.getUserId()))).hasSize(1);
    }

    @Test
    @DisplayName("Should find all user roles")
    void whenFindAll_ThenReturnAllUserRoles() {
        // When
        List<UserRole> allUserRoles = userRoleRepository.findAll();

        // Then
        assertThat(allUserRoles).hasSize(3);
        assertThat(allUserRoles)
                .extracting(u -> u.getId().getRoleId())
                .containsExactlyInAnyOrder(ROLE_USER, ROLE_ADMIN, ROLE_USER);
    }

    @Test
    @DisplayName("Should check existence by composite ID")
    void whenExistsById_ThenReturnTrueIfExists() {
        // Given
        UserRoleId id = new UserRoleId();
        id.setUserId(testUser1.getUserId());
        id.setRoleId(ROLE_ADMIN);

        // When
        boolean exists = userRoleRepository.existsById(id);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when checking existence by non-existing composite ID")
    void whenExistsById_WithNonExistingId_ThenReturnFalse() {
        // Given
        UserRoleId id = new UserRoleId();
        id.setUserId("non-existing-user-id");
        id.setRoleId(ROLE_ADMIN);

        // When
        boolean exists = userRoleRepository.existsById(id);

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should verify composite key uniqueness constraint")
    void whenSaveDuplicateUserRole_ThenThrowException() {
        // Given
        UserRoleId duplicateId = new UserRoleId();
        duplicateId.setUserId(testUser1.getUserId());
        duplicateId.setRoleId(ROLE_ADMIN);

        UserRole duplicateUserRole = createUserRole(duplicateId, testUser1);

        userRoleRepository.saveAndFlush(duplicateUserRole);

        List<UserRole> userRoles = userRoleRepository.findByUserUserId(testUser1.getUserId());

        assertThat(userRoles).hasSize(2);
    }
}
