package com.lynus.cs203.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lynus.cs203.Cs203Application;
import com.lynus.cs203.entities.*;
import com.lynus.cs203.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected User testUser;
    protected User adminUser;

    @BeforeEach
    void baseSetUp() {
        userRepository.deleteAll();
        setupTestUsers();
    }

    protected void setupTestUsers() {
        // Create regular user
        testUser = new User();
        testUser.setEmail("user@example.com");
        testUser.setPassword(passwordEncoder.encode("UserPassword@123"));
        testUser.setIsActive(true);
        testUser.setCreatedAt(Instant.now());
        testUser.setUpdatedAt(Instant.now());

        UserProfile userProfile = new UserProfile();
        userProfile.setFirstName("Test");
        userProfile.setLastName("User");
        userProfile.setUser(testUser);
        testUser.setUserProfile(userProfile);

        testUser = userRepository.save(testUser);

        // Create admin user
        adminUser = new User();
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword(passwordEncoder.encode("AdminPassword@123"));
        adminUser.setIsActive(true);
        adminUser.setCreatedAt(Instant.now());
        adminUser.setUpdatedAt(Instant.now());

        UserProfile adminProfile = new UserProfile();
        adminProfile.setFirstName("Admin");
        adminProfile.setLastName("User");
        adminProfile.setUser(adminUser);
        adminUser.setUserProfile(adminProfile);

        adminUser = userRepository.save(adminUser);

        // Set up roles
        UserRole userRole = createUserRole(testUser, Role.USER);
        UserRole adminRole = createUserRole(adminUser, Role.ADMIN);
        UserRole adminUserRole = createUserRole(adminUser, Role.USER);

        testUser.setUserRoles(new HashSet<>(Set.of(userRole)));
        adminUser.setUserRoles(new HashSet<>(Set.of(adminRole, adminUserRole)));

        userRepository.saveAll(List.of(testUser, adminUser));
    }

    protected UserRole createUserRole(User user, Role role) {
        UserRole userRole = new UserRole();
        UserRoleId userRoleId = new UserRoleId();
        userRoleId.setUserId(user.getUserId());
        userRoleId.setRoleId((short) role.getId());
        userRole.setId(userRoleId);
        userRole.setUser(user);
        userRole.setRole(role);
        return userRole;
    }

    protected UserDetails createUserDetails(String userId, String... roles) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(userId)
                .password("password")
                .roles(roles)
                .build();
    }
}
