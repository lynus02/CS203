package com.lynus.cs203.integration;

import com.lynus.cs203.entities.User;
import com.lynus.cs203.repositories.UserProfileRepository;
import com.lynus.cs203.repositories.UserRepository;
import com.lynus.cs203.repositories.UserRoleRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

import java.sql.Connection;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Application Startup Integration Tests")
public class ApplicationStartUpTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Test
    @Order(1)
    @DisplayName("Spring application context should load successfully")
    void contextLoads() {
        assertNotNull(applicationContext, "Application context should have loaded");
        assertTrue(applicationContext.getBeanDefinitionCount() > 0,
                "Application context should contain beans");
    }

    @Test
    @Order(2)
    @DisplayName("Database connection should be established")
    void databaseConnectionEstablished() throws Exception {
        assertNotNull(dataSource, "Database connection should be established");

        try (Connection connection = dataSource.getConnection()) {
            assertNotNull(connection, "Database connection should not be null");
            assertFalse(connection.isClosed(), "Database connection should be open");
            assertTrue(connection.isValid(5), "Database connection should be valid");
        }
    }

    @Test
    @Order(3)
    @DisplayName("JPA repositories should be functional")
    void repositoriesAreFunctional() {
        assertNotNull(userRepository, "UserRepository should be autowired");
        assertNotNull(userRoleRepository, "RoleRepository should be autowired");

        // Test basic repository operations
        long userCount = userRepository.count();
        long roleCount = userRoleRepository.count();

        assertTrue(userCount >= 0, "User count should be non-negative");
        assertTrue(roleCount >= 0, "Role count should be non-negative");
    }

    @Test
    @Order(4)
    @DisplayName("Database schema should support basic entity operations")
    @Transactional
    void basicEntityOperationsWork() {
        // Create and save a test user to verify schema
        User testUser = new User();
        testUser.setEmail("startup-test@example.com");
        testUser.setPassword("test-password");
        testUser.setIsActive(true);
        testUser.setCreatedAt(Instant.now());
        testUser.setUpdatedAt(Instant.now());

        User savedUser = userRepository.save(testUser);

        assertNotNull(savedUser.getUserId(), "User ID should be generated");
        assertEquals("startup-test@example.com", savedUser.getEmail());
        assertTrue(savedUser.getIsActive());

        // Verify we can retrieve the user
        User retrievedUser = userRepository.findById(savedUser.getUserId()).orElse(null);
        assertNotNull(retrievedUser, "Should be able to retrieve saved user");
        assertEquals(savedUser.getEmail(), retrievedUser.getEmail());
    }

    @Test
    @Order(5)
    @DisplayName("Security configuration should be loaded")
    void securityConfigurationLoaded() {
        // Verify security-related beans are present
        assertTrue(applicationContext.containsBean("passwordEncoder"),
                "PasswordEncoder bean should be configured");

        // Check if security filter chain is configured
        String[] securityBeans = applicationContext.getBeanNamesForType(
                org.springframework.security.web.SecurityFilterChain.class);
        assertTrue(securityBeans.length > 0, "Security filter chain should be configured");
    }
}
