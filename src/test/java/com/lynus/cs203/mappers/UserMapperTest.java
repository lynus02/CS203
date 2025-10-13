package com.lynus.cs203.mappers;

import com.lynus.cs203.dtos.request.CreateUserRequest;
import com.lynus.cs203.dtos.request.UpdateUserRequest;
import com.lynus.cs203.dtos.response.UserDto;
import com.lynus.cs203.entities.User;
import com.lynus.cs203.entities.UserProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class UserMapperTest {

    private final UserMapperImpl userMapper = new UserMapperImpl(); // Your mapper implementation

    @Test
    void toEntity_WhenValidCreateUserRequest_ShouldMapCorrectly() {
        // Arrange
        CreateUserRequest request = CreateUserRequest.builder()
                .email("test@example.com")
                .password("rawPassword") // Should not be mapped directly
                .firstName("John")
                .lastName("Doe")
                .build();

        // Act
        User user = userMapper.toEntity(request);

        // Assert
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        // Password should be handled by service, not mapper
        assertThat(user.getPassword()).isNull();
        // Timestamps and active status should be set by service
        assertThat(user.getCreatedAt()).isNull();
        assertThat(user.getUpdatedAt()).isNull();
        assertThat(user.getIsActive()).isNull();
    }

    @Test
    void update_WhenUpdateUserRequest_ShouldUpdateUserFields() {
        // Arrange
        User existingUser = new User();
        existingUser.setUserId("123");
        existingUser.setEmail("old@example.com");
        existingUser.setIsActive(true);

        UpdateUserRequest request = UpdateUserRequest.builder()
                .email("new@example.com")
                .build();

        // Act
        userMapper.update(request, existingUser);

        // Assert
        assertThat(existingUser.getEmail()).isEqualTo("new@example.com");
        // Unchanged fields should remain
        assertThat(existingUser.getUserId()).isEqualTo("123");
        assertThat(existingUser.getIsActive()).isTrue();
    }

    @Test
    void toDto_WhenUserWithProfile_ShouldMapToDtoCorrectly() {
        // Arrange
        User user = new User();
        user.setUserId("123");
        user.setEmail("test@example.com");
        user.setIsActive(true);

        UserProfile profile = new UserProfile();
        profile.setFirstName("John");
        profile.setLastName("Doe");
        user.setUserProfile(profile);

        // Act
        UserDto dto = userMapper.toDto(user);

        // Assert
        assertThat(dto.getUserId()).isEqualTo("123");
        assertThat(dto.getEmail()).isEqualTo("test@example.com");
        assertThat(dto.getFirstName()).isEqualTo("John");
        assertThat(dto.getLastName()).isEqualTo("Doe");
        assertThat(dto.getIsActive()).isTrue();
    }
}