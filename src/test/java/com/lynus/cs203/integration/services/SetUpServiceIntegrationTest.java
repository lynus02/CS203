package com.lynus.cs203.integration.services;

import com.lynus.cs203.dtos.request.CreateUserRequest;
import com.lynus.cs203.dtos.response.AdminCreationResponse;
import com.lynus.cs203.dtos.response.SetupStatusResponse;
import com.lynus.cs203.entities.*;
import com.lynus.cs203.exceptions.AdminAlreadyExistsException;
import com.lynus.cs203.repositories.UserRepository;
import com.lynus.cs203.repositories.UserRoleRepository;
import com.lynus.cs203.services.SetupService;
import com.lynus.cs203.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:mysql://localhost:3306/tariff",
        "spring.datasource.username=root",
        "spring.datasource.password=password123"
})
@Transactional
@DisplayName("Setup Service Integration Tests")
public class SetUpServiceIntegrationTest {

    @Autowired
    private SetupService setupService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final Short ROLE_USER = 1;
    private static final Short ROLE_ADMIN = 2;

    private User testAdminUser;
    private User testUser;
    private UserRole testAdminUserRole;
    private UserRole testUserRole;

    @BeforeEach
    void setUp() {
        userRoleRepository.deleteAll();
        userRepository.deleteAll();

        testAdminUser = createUser("admin@example.com", "Password@123");
        testUser = createUser("test@example.com", "Password@123");

        userRepository.saveAll(List.of(testAdminUser, testUser));

        UserRoleId id1 = new UserRoleId();
        id1.setUserId(testAdminUser.getUserId());
        id1.setRoleId(ROLE_ADMIN);

        UserRoleId id2 = new UserRoleId();
        id2.setUserId(testUser.getUserId());
        id2.setRoleId(ROLE_USER);

        testAdminUserRole = createUserRole(id1, testAdminUser);
        testUserRole = createUserRole(id2, testUser);

        userRoleRepository.saveAll(List.of(testAdminUserRole, testUserRole));
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
    @DisplayName("Should create first admin successfully when no admin exists")
    void whenCreateFirstAdminWhenNoAdminExists_thenCreateAdmin() {
        // Given
        userRoleRepository.deleteAll();

        CreateUserRequest createUserRequest = CreateUserRequest.builder()
                .email("firstadmin@example.com")
                .password("Password@123")
                .firstName("First")
                .lastName("Admin")
                .build();

        // When
        AdminCreationResponse response = setupService.createFirstAdmin(createUserRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isNotNull();
        assertThat(response.getEmail()).isEqualTo("firstadmin@example.com");
        assertThat(response.getMessage()).contains("successfully");

        // Verify user was created
        User createdUser = userRepository.findByEmail("firstadmin@example.com").orElse(null);
        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getIsActive()).isTrue();

        // Verify admin role was assigned
        UserRoleId expectedId = new UserRoleId();
        expectedId.setUserId(createdUser.getUserId());
        expectedId.setRoleId(ROLE_ADMIN);

        Optional<UserRole> userRole = userRoleRepository.findById(expectedId);
        assertThat(userRole).isPresent();
        assertThat(userRole.get().getId().getRoleId()).isEqualTo(expectedId.getRoleId());
    }

    @Test
    @DisplayName("Should throw AdminAlreadyExistsException when admin already exists")
    void whenCreateFirstAdminWhenAdminExists_thenThrowAdminAlreadyExistsException() {
        // Given
        CreateUserRequest request = CreateUserRequest.builder()
                .email("newadmin@example.com")
                .password("Password@123")
                .firstName("New")
                .lastName("Admin")
                .build();

        AdminAlreadyExistsException exception = assertThrows(
                AdminAlreadyExistsException.class,
                () -> setupService.createFirstAdmin(request)
        );

        assertThat(exception.getMessage()).contains("Admin setup already completed");

        // Verify no new user was created
        long userCount = userRepository.count();
        assertThat(userCount).isEqualTo(2); // Only the two users from setup
    }

    @Test
    @DisplayName("Should return setup complete when admin exists")
    void whenGetSetupStatusWhenAdminExists_thenReturnSetupComplete() {
        SetupStatusResponse setupStatusResponse = setupService.getSetupStatus();

        assertThat(setupStatusResponse).isNotNull();
        assertThat(setupStatusResponse.isSetupComplete()).isTrue();
        assertThat(setupStatusResponse.getMessage()).contains("Admin user exists");
    }

    @Test
    @DisplayName("Should return setup incomplete when no admin exists")
    void whenGetSetupStatusWhenNoAdminExist_thenReturnSetupIncomplete() {
        userRoleRepository.delete(testAdminUserRole);

        SetupStatusResponse setupStatusResponse = setupService.getSetupStatus();

        assertThat(setupStatusResponse).isNotNull();
        assertThat(setupStatusResponse.isSetupComplete()).isFalse();
        assertThat(setupStatusResponse.getMessage()).contains("No admin user found");
    }

    @Test
    @DisplayName("Should not allow admin creation when there is an admin")
    void whenAdminExists_thenPreventAdminCreation() {
        CreateUserRequest request = CreateUserRequest.builder()
                .email("newuser@example.com")
                .password("Password@123")
                .firstName("Admin")
                .lastName("User")
                .build();

        AdminAlreadyExistsException exception = assertThrows(AdminAlreadyExistsException.class,
                () -> setupService.createFirstAdmin(request));

        assertThat(exception.getMessage()).contains("Admin setup already completed");

        assertThat(userRepository.count()).isEqualTo(2);

        assertThat(userRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should return setup incomplete when there are no users")
    void whenNoUsersExist_thenSetupIncomplete() {
        userRoleRepository.deleteAll();
        userRepository.deleteAll();

        SetupStatusResponse setupStatusResponse = setupService.getSetupStatus();

        assertThat(setupStatusResponse).isNotNull();
        assertThat(setupStatusResponse.isSetupComplete()).isFalse();
        assertThat(setupStatusResponse.getMessage()).contains("No admin user found");
    }

    @Test
    @DisplayName("Should allow admin creation when only users exists")
    void whenOnlyUsersExist_thenAllowAdminCreation() {
        userRoleRepository.delete(testAdminUserRole);

        CreateUserRequest createUserRequest = CreateUserRequest.builder()
                .email("firstadmin@example.com")
                .password("Password@123")
                .firstName("Admin")
                .lastName("User")
                .build();

        AdminCreationResponse adminCreationResponse = setupService.createFirstAdmin(createUserRequest);

        assertThat(adminCreationResponse).isNotNull();
        assertThat(adminCreationResponse.getEmail()).isEqualTo("firstadmin@example.com");

        assertThat(userRepository.count()).isEqualTo(3);

        User adminUser = userRepository.findByEmail("firstadmin@example.com").orElse(null);
        assertThat(adminUser).isNotNull();

        UserRoleId expectedId = new UserRoleId();
        expectedId.setUserId(adminUser.getUserId());
        expectedId.setRoleId(ROLE_ADMIN);

        Optional<UserRole> adminRole = userRoleRepository.findById(expectedId);
        assertThat(adminRole).isPresent();
        assertThat(adminRole.get().getId().getRoleId()).isEqualTo(expectedId.getRoleId());
    }
}
