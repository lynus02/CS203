package com.lynus.cs203.mappers;

import com.lynus.cs203.dtos.request.CreateUserRequest;
import com.lynus.cs203.dtos.request.UpdateUserRequest;
import com.lynus.cs203.dtos.response.UserDto;
import com.lynus.cs203.entities.Role;
import com.lynus.cs203.entities.User;
import com.lynus.cs203.entities.UserProfile;
import com.lynus.cs203.entities.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("User Mapper Unit Test")
class UserMapperTest {

    private final UserMapperImpl userMapper = new UserMapperImpl();

    @Test
    @DisplayName("Should map CreateUserRequest to User entity correctly")
    void toEntity_WhenValidCreateUserRequest_ShouldMapCorrectly() {
        // Arrange
        CreateUserRequest request = CreateUserRequest.builder()
                .email("test@example.com")
                .password("Password@123")
                .firstName("John")
                .lastName("Doe")
                .build();

        // Act
        User user = userMapper.toEntity(request);

        // Assert
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getPassword()).isNull();
        assertThat(user.getCreatedAt()).isNull();
        assertThat(user.getUpdatedAt()).isNull();
        assertThat(user.getIsActive()).isFalse();
        assertThat(user.getUserProfile()).isNull();
    }

    @Test
    @DisplayName("Should update User entity fields from UpdateUserRequest")
    void update_WhenUpdateUserRequest_ShouldUpdateUserFields() {
        // Arrange
        User existingUser = new User();
        existingUser.setUserId("userId");
        existingUser.setEmail("old@example.com");
        existingUser.setIsActive(true);
        existingUser.setUpdatedAt(Instant.now().minusSeconds(3600));

        UpdateUserRequest request = UpdateUserRequest.builder()
                .email("new@example.com")
                .build();

        // Act
        userMapper.update(request, existingUser);

        // Assert
        assertThat(existingUser.getEmail()).isEqualTo("new@example.com");
        assertThat(existingUser.getUpdatedAt()).isNotNull();
        // Unchanged fields should remain
        assertThat(existingUser.getUserId()).isEqualTo("userId");
        assertThat(existingUser.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("Should not update email when UpdateUserRequest email is null")
    void update_WhenUpdateUserRequestEmailIsNull_ShouldNotUpdateEmail() {
        // Arrange
        User existingUser = new User();
        existingUser.setUserId("userId");
        existingUser.setEmail("original@example.com");
        existingUser.setIsActive(true);
        existingUser.setUpdatedAt(Instant.now().minusSeconds(3600));

        UpdateUserRequest request = UpdateUserRequest.builder()
                .email(null)
                .firstName("John")
                .lastName("Doe")
                .build();

        // Act
        userMapper.update(request, existingUser);

        // Assert
        assertThat(existingUser.getEmail()).isEqualTo("original@example.com");
        assertThat(existingUser.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should map User entity to UserDto correctly")
    void toDto_WhenUserWithProfile_ShouldMapToDtoCorrectly() {
        // Arrange
        User user = new User();
        user.setUserId("userId");
        user.setEmail("test@example.com");
        user.setIsActive(true);
        user.setCreatedAt(Instant.now().minusSeconds(3600));
        user.setUpdatedAt(Instant.now());

        UserProfile profile = new UserProfile();
        profile.setFirstName("John");
        profile.setLastName("Doe");
        user.setUserProfile(profile);

        // Act
        UserDto dto = userMapper.toDto(user);

        // Assert
        assertThat(dto.getUserId()).isEqualTo("userId");
        assertThat(dto.getEmail()).isEqualTo("test@example.com");
        assertThat(dto.getFirstName()).isEqualTo("John");
        assertThat(dto.getLastName()).isEqualTo("Doe");
        assertThat(dto.getIsActive()).isTrue();
        assertThat(dto.getRoles()).isNotNull();
    }

    @Test
    @DisplayName("Should return null when CreateUserRequest is null")
    void toEntity_WhenNullRequest_ShouldReturnNull() {
        // Act
        User user = userMapper.toEntity(null);

        // Assert
        assertThat(user).isNull();
    }

    @Test
    @DisplayName("Should not change User entity when UpdateUserRequest is null")
    void update_WhenNullRequest_ShouldNotChangeUser() {
        // Arrange
        User existingUser = new User();
        existingUser.setUserId("userId");
        existingUser.setEmail("original@example.com");
        existingUser.setIsActive(true);

        // Act
        userMapper.update(null, existingUser);

        // Assert
        assertThat(existingUser.getEmail()).isEqualTo("original@example.com");
        assertThat(existingUser.getUserId()).isEqualTo("userId");
        assertThat(existingUser.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("Should return null when User is null")
    void toDto_WhenUserIsNull_ShouldReturnNull() {
        // Act
        UserDto dto = userMapper.toDto(null);

        // Assert
        assertThat(dto).isNull();
    }

    @Test
    @DisplayName("Should handle null UserProfile when mapping to UserDto")
    void toDto_WhenUserWithoutProfile_ShouldHandleNullProfile() {
        // Arrange
        User user = new User();
        user.setUserId("userId");
        user.setEmail("test@example.com");
        user.setIsActive(true);

        // Act
        UserDto dto = userMapper.toDto(user);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.getUserId()).isEqualTo("userId");
        assertThat(dto.getEmail()).isEqualTo("test@example.com");
        assertThat(dto.getFirstName()).isNull();
        assertThat(dto.getLastName()).isNull();
        assertThat(dto.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("Should return empty list when user has no roles")
    void getUserRoles_WhenUserHasNoRoles_ShouldReturnEmptyList() {
        // Arrange
        User user = new User();
        user.setUserRoles(null);

        // Act
        List<String> roles = userMapper.getUserRoles(user);

        // Assert
        assertThat(roles).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should extract role names from user roles")
    void getUserRoles_WhenUserHasRoles_ShouldReturnRoleNames() {
        // Arrange
        User user = new User();

        UserRole userUserRole = new UserRole();
        userUserRole.setRole(Role.USER);

        UserRole userAdminRole = new UserRole();
        userAdminRole.setRole(Role.ADMIN);

        user.setUserRoles(Set.of(userUserRole, userAdminRole));

        // Act
        List<String> roles = userMapper.getUserRoles(user);

        // Assert
        assertThat(roles).containsExactlyInAnyOrder("USER", "ADMIN");
    }
}