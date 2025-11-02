package com.lynus.cs203.services;

import com.lynus.cs203.dtos.request.ChangePasswordRequest;
import com.lynus.cs203.dtos.request.CreateUserRequest;
import com.lynus.cs203.dtos.request.UpdateUserRequest;
import com.lynus.cs203.dtos.response.PasswordChangeResponse;
import com.lynus.cs203.dtos.response.UserDto;
import com.lynus.cs203.entities.Role;
import com.lynus.cs203.entities.User;
import com.lynus.cs203.entities.UserProfile;
import com.lynus.cs203.entities.UserRole;
import com.lynus.cs203.exceptions.EmailAlreadyExistsException;
import com.lynus.cs203.exceptions.InvalidPasswordException;
import com.lynus.cs203.exceptions.UserNotFoundException;
import com.lynus.cs203.mappers.UserMapper;
import com.lynus.cs203.repositories.UserProfileRepository;
import com.lynus.cs203.repositories.UserRepository;
import com.lynus.cs203.repositories.UserRoleRepository;
import io.swagger.v3.oas.annotations.media.Schema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.shaded.org.checkerframework.checker.fenum.qual.SwingHorizontalOrientation;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("User Service Unit Test")
class UserServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @InjectMocks
    private UserService userService;

    // ========== High-Priority Tests ==========
    @Test
    @DisplayName("Should create user when valid request is provided")
    void createUser_WhenValidRequest_ShouldCreateUser() {
        // Arrange
        CreateUserRequest request = CreateUserRequest.builder()
                .email("test@example.com")
                .password("Password@123")
                .firstName("John")
                .lastName("Doe")
                .build();

        User mappedUser = new User();
        mappedUser.setEmail(request.getEmail());

        User savedUser = new User();
        savedUser.setUserId("userId");
        savedUser.setEmail(request.getEmail());
        savedUser.setPassword("encodedPassword");
        savedUser.setIsActive(true);

        UserProfile savedProfile = new UserProfile();
        savedProfile.setFirstName(request.getFirstName());
        savedProfile.setLastName(request.getLastName());
        savedProfile.setUser(savedUser);

        savedUser.setUserProfile(savedProfile);

        // Set up mocks
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(mappedUser);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(savedProfile);
        when(userRepository.findById("userId")).thenReturn(Optional.of(savedUser));

        // Act
        User result = userService.createUser(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo("userId");
        assertThat(result.getEmail()).isEqualTo(request.getEmail());

        // Verify interactions
        verify(userRepository).existsByEmail(request.getEmail());
        verify(userMapper).toEntity(request);
        verify(passwordEncoder).encode(request.getPassword());
        verify(userRepository).save(any(User.class));
        verify(userProfileRepository).save(any(UserProfile.class));
        verify(userRepository).findById("userId");
        verify(userRoleRepository).save(any(UserRole.class));
    }

    @Test
    @DisplayName("Should throw exception when email already exists during user creation")
    void createUser_WhenEmailExists_ShouldThrowException() {
        // Arrange
        CreateUserRequest request = CreateUserRequest.builder()
                .email("existing@example.com")
                .password("Password@123")
                .firstName("John")
                .lastName("Doe")
                .build();

        // Set up mock
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("Email already exists");

        // Verify interaction
        verify(userRepository, never()).save(any(User.class));
        verify(userProfileRepository, never()).save(any(UserProfile.class));
        verify(userMapper, never()).toEntity(request);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRoleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should change password when valid request is provided")
    void changePassword_WhenValidRequest_ShouldChangePassword() {
        // Arrange
        String userId = "userId";
        String oldPassword = "oldPassword";
        String newPassword = "newPassword";
        String encodedNewPassword = "encodedNewPassword";

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword(oldPassword)
                .newPassword(newPassword)
                .build();

        User user = new User();
        user.setUserId(userId);
        user.setEmail("test@example.com");
        user.setPassword("encodedOldPassword");

        // Set up mocks
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(oldPassword, user.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedNewPassword);
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        PasswordChangeResponse response = userService.changePassword(userId, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("Password changed successfully");
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(user.getPassword()).isEqualTo(encodedNewPassword);

        // Verify
        verify(userRepository).findById(userId);
        verify(passwordEncoder).matches(oldPassword, "encodedOldPassword");
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Should throw exception when old password is invalid during password change")
    void changePassword_WhenInvalidOldPassword_ShouldThrowException() {
        // Arrange
        String userId = "userId";
        String wrongOldPassword = "wrongOldPassword";
        String newPassword = "newPassword";

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword(wrongOldPassword)
                .newPassword(newPassword)
                .build();

        User user = new User();
        user.setUserId(userId);
        user.setEmail("test@example.com");
        user.setPassword("encodedOldPassword");

        // Set up mocks
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(wrongOldPassword, user.getPassword())).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> userService.changePassword(userId, request))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("Old password is incorrect");

        // Verify
        verify(userRepository).findById(userId);
        verify(passwordEncoder).matches(wrongOldPassword, user.getPassword());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when user not found during password change")
    void changePassword_WhenUserNotFound_ShouldThrowException() {
        // Arrange
        String userId = "nonExistentUserId";

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("oldPassword")
                .newPassword("newPassword")
                .build();

        // Set up mock
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.changePassword(userId, request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found");

        // Verify interaction
        verify(userRepository).findById(userId);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should update user and profile when all fields are provided")
    void updateUser_WhenUserExistsWithAllFields_ShouldUpdateUserAndProfile() {
        // Arrange
        String userId = "userId";
        UpdateUserRequest request = UpdateUserRequest.builder()
                .firstName("UpdatedFirstName")
                .lastName("UpdatedLastName")
                .email("updated@example.com")
                .build();

        User existingUser = new User();
        existingUser.setUserId(userId);
        existingUser.setEmail("original@example.com");

        UserProfile existingProfile = new UserProfile();
        existingProfile.setFirstName("OriginalFirstName");
        existingProfile.setLastName("OriginalLastName");
        existingUser.setUserProfile(existingProfile);

        // Set up mocks
        when(userRepository.findById(userId))
                .thenReturn(Optional.of(existingUser))
                .thenReturn(Optional.of(existingUser)); // For the second call after save
        when(userRepository.save(existingUser)).thenReturn(existingUser);
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(existingProfile);

        // Use ArgumentCaptor to capture the updated user
        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);

        // Act
        User result = userService.updateUser(userId, request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);

        // Verify interactions
        verify(userRepository, times(2)).findById(userId);
        verify(userMapper).update(request, existingUser);
        verify(userProfileRepository).save(captor.capture());
        verify(userRepository).save(existingUser);

        // Verify that the profile was updated
        UserProfile updatedProfile = captor.getValue();
        assertThat(updatedProfile.getFirstName()).isEqualTo(request.getFirstName());
        assertThat(updatedProfile.getLastName()).isEqualTo(request.getLastName());
    }

    @Test
    @DisplayName("Should update only first name when user exists with only first name provided")
    void updateUser_WhenUserExistsWithOnlyFirstName_ShouldUpdateOnlyFirstName() {
        // Arrange
        String userId = "userId";
        UpdateUserRequest request = UpdateUserRequest.builder()
                .firstName("UpdatedFirstName")
                .build();

        User existingUser = new User();
        existingUser.setUserId(userId);
        existingUser.setEmail("original@example.com");

        UserProfile existingProfile = new UserProfile();
        existingProfile.setFirstName("OriginalFirstName");
        existingProfile.setLastName("OriginalLastName");
        existingUser.setUserProfile(existingProfile);

        // Set up mocks
        when(userRepository.findById(userId))
                .thenReturn(Optional.of(existingUser))
                .thenReturn(Optional.of(existingUser)); // For the second call after save
        when(userRepository.save(existingUser)).thenReturn(existingUser);
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(existingProfile);

        // Use ArgumentCaptor to capture the updated user profile
        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);

        // Act
        User result = userService.updateUser(userId, request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);

        // Verify interactions
        verify(userRepository, times(2)).findById(userId);
        verify(userMapper).update(request, existingUser);
        verify(userProfileRepository).save(captor.capture());
        verify(userRepository).save(existingUser);

        // Verify that only the first name was updated
        UserProfile updatedProfile = captor.getValue();
        assertThat(updatedProfile.getFirstName()).isEqualTo(request.getFirstName());
        assertThat(updatedProfile.getLastName()).isEqualTo(existingProfile.getLastName());
    }

    @Test
    @DisplayName("Should throw exception when user not found during update")
    void updateUser_WhenUserNotFound_ShouldThrowException() {
        // Arrange
        String userId = "nonExistentUserId";
        UpdateUserRequest request = UpdateUserRequest.builder()
                .firstName("UpdatedFirstName")
                .lastName("UpdatedLastName")
                .email("updated@example.com")
                .build();

        // Set up mock
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUser(userId, request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found");

        // Verify interaction
        verify(userRepository).findById(userId);
        verify(userMapper, never()).update(any(), any());
        verify(userProfileRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when updating email to existing email")
    void updateUser_WhenEmailExists_ShouldThrowException() {
        // Arrange
        String userId = "userId";
        UpdateUserRequest request = UpdateUserRequest.builder()
                .email("existing@example.com")
                .build();

        User existingUser = new User();
        existingUser.setUserId(userId);
        existingUser.setEmail("original@example.com");

        // Set up mocks
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUser(userId, request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("Email already exists");

        // Verify interactions
        verify(userRepository).findById(userId);
        verify(userRepository).existsByEmail(request.getEmail());
        verify(userMapper, never()).update(any(), any());
        verify(userProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete user when user exists")
    void deleteUser_WhenUserExists_ShouldDeleteUser() {
        // Arrange
        String userId = "userId";

        User user = new User();
        user.setUserId(userId);
        user.setEmail("test@example.com");

        // Set up mocks
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Use ArgumentCaptor
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        // Act
        userService.deleteUser(userId);

        // Assert
        verify(userRepository).findById(userId);
        verify(userRepository).delete(captor.capture());

        // Verify
        User deletedUser = captor.getValue();
        assertThat(deletedUser.getUserId()).isEqualTo(userId);
        assertThat(deletedUser.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should throw exception when user not found during deletion")
    void deleteUser_WhenUserNotFound_ShouldThrowException() {
        // Arrange
        String userId = "nonExistentUserId";

        // Set up mocks
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.deleteUser(userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found");

        // Verify
        verify(userRepository).findById(userId);
        verify(userRepository, never()).delete(any(User.class));
    }

    // ========== Medium-Priority Tests ==========
    @Test
    @DisplayName("Should get user by email when user exists")
    void getUserByEmail_WhenUserExists_ShouldReturnUser() {
        // Arrange
        String email = "test@example.com";
        User expectedUser = new User();
        expectedUser.setUserId("userId");
        expectedUser.setEmail(email);

        // Set up mocks
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(expectedUser));

        // Act
        User result = userService.getUserByEmail(email);

        // Assert
        assertThat(result).isEqualTo(expectedUser);

        // Verify
        verify(userRepository).findByEmail(email);
    }

    @Test
    @DisplayName("Should throw exception when user not found by email")
    void getUserByEmail_WhenUserNotFound_ShouldThrowException() {
        // Arrange
        String email = "nonExistentEmail";

        // Set up mocks
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.getUserByEmail(email))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found");

        // Verify
        verify(userRepository).findByEmail(email);
    }

    @Test
    @DisplayName("Should get user by ID when user exists")
    void getUserById_WhenUserExists_ShouldReturnUser() {
        // Arrange
        String userId = "userId";
        User expectedUser = new User();
        expectedUser.setUserId("userId");
        expectedUser.setEmail("test@example.com");

        // Set up mocks
        when(userRepository.findById(userId)).thenReturn(Optional.of(expectedUser));

        // Act
        User result = userService.getUserById(userId);

        // Assert
        assertThat(result).isEqualTo(expectedUser);

        // Verify
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("Should throw exception when user not found by ID")
    void getUserById_WhenUserNotFound_ShouldThrowException() {
        // Arrange
        String userId = "nonexistentUserId";

        // Set up mocks
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.getUserById(userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found");

        // Verify
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("Should assign role when user does not have the role")
    void assignRole_WhenUserDoesNotHaveRole_ShouldAssignRole() {
        // Arrange
        String userId = "userId";
        Role role = Role.ADMIN;

        User user = new User();
        user.setUserId(userId);
        user.setEmail("test@example.com");

        // Set up mocks
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRoleRepository.existsByUserUserIdAndRole(userId, role)).thenReturn(false);
        when(userRoleRepository.save(any(UserRole.class))).thenReturn(new UserRole());

        // Use ArgumentCaptor
        ArgumentCaptor<UserRole> captor = ArgumentCaptor.forClass(UserRole.class);

        // Act
        userService.assignRole(userId, role);

        // Assert
        verify(userRepository).findById(userId);
        verify(userRoleRepository).existsByUserUserIdAndRole(userId, role);
        verify(userRoleRepository).save(captor.capture());

        // Verify
        UserRole updatedRole = captor.getValue();
        assertThat(updatedRole.getUser()).isEqualTo(user);
        assertThat(updatedRole.getRole()).isEqualTo(role);
        assertThat(updatedRole.getAssignedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should not assign role when user already has the role")
    void assignRole_WhenUserHasRole_ShouldNotAssignRole() {
        // Arrange
        String userId = "userId";
        Role role = Role.ADMIN;

        User user = new User();
        user.setUserId(userId);
        user.setEmail("test@example.com");

        // Set up mocks
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRoleRepository.existsByUserUserIdAndRole(userId, role)).thenReturn(true);

        // Act
        userService.assignRole(userId, role);

        // Assert
        verify(userRepository).findById(userId);
        verify(userRoleRepository).existsByUserUserIdAndRole(userId, role);
        verify(userRoleRepository, never()).save(any(UserRole.class));
    }

    @Test
    @DisplayName("Should get user roles when user exists with roles")
    void getUserRoles_WhenUserExistsWithRoles_ShouldReturnRoles() {
        // Arrange
        String userId = "userId";

        User user = new User();
        user.setUserId(userId);
        user.setEmail("test@example.com");

        UserRole userRole1 = new UserRole();
        userRole1.setRole(Role.USER);

        UserRole userRole2 = new UserRole();
        userRole2.setRole(Role.ADMIN);

        List<UserRole> userRoles = List.of(userRole1, userRole2);

        // Set up mocks
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRoleRepository.findByUserUserId(userId)).thenReturn(userRoles);

        // Act
        List<String> result = userService.getUserRoles(userId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly("USER", "ADMIN");

        // Verify
        verify(userRepository).findById(userId);
        verify(userRoleRepository).findByUserUserId(userId);
    }

    @Test
    @DisplayName("Should return empty list when user has no roles")
    void getUserRoles_WhenUserHasNoRoles_ShouldReturnEmptyList() {
        // Arrange
        String userId = "userId";

        User user = new User();
        user.setUserId(userId);
        user.setEmail("test@example.com");

        List<UserRole> userRoles = List.of();

        // Set up mocks
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRoleRepository.findByUserUserId(userId)).thenReturn(userRoles);

        // Act
        List<String> result = userService.getUserRoles(userId);

        // Assert
        assertThat(result).isEmpty();

        // Verify
        verify(userRepository).findById(userId);
        verify(userRoleRepository).findByUserUserId(userId);
    }

    @Test
    @DisplayName("Should throw exception when user not found during getUserRoles")
    void getUserRoles_WhenUserNotFound_ShouldThrowException() {
        // Arrange
        String userId = "userId";

        // Set up mock
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.getUserRoles(userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found");

        // Verify
        verify(userRepository).findById(userId);
        verify(userRoleRepository, never()).findByUserUserId(anyString());
    }

    // ========== Low-Priority Tests ==========
    @Test
    @DisplayName("Should get all users with valid sort")
    void getAllUsers_WithValidSort_ShouldReturnUsers() {
        // Arrange
        String sort = "email";

        User user1 = new User();
        user1.setUserId("user1");
        user1.setEmail("alice@example.com");

        User user2 = new User();
        user2.setUserId("user2");
        user2.setEmail("bob@example.com");

        List<User> expectedUsers = List.of(user1, user2);

        // Set up mocks
        when(userRepository.findAll(Sort.by(sort))).thenReturn(expectedUsers);

        // Act
        List<User> result = userService.getAllUsers(sort);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).isEqualTo(expectedUsers);

        // Verify
        verify(userRepository).findAll(Sort.by(sort));
    }

    @Test
    @DisplayName("Should get all users with invalid sort and use default sort")
    void getAllUsers_WithInvalidSort_ShouldUseDefaultSort() {
        // Arrange
        String invalidSort = "invalidField";

        User user1 = new User();
        user1.setUserId("user1");
        user1.setEmail("alice@example.com");

        User user2 = new User();
        user2.setUserId("user2");
        user2.setEmail("bob@example.com");

        List<User> expectedUsers = List.of(user1, user2);

        // Set up mocks
        when(userRepository.findAll(Sort.by("createdAt"))).thenReturn(expectedUsers);

        // Act
        List<User> result = userService.getAllUsers(invalidSort);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).isEqualTo(expectedUsers);

        // Verify
        verify(userRepository).findAll(Sort.by("createdAt"));
        verify(userRepository, never()).findAll(Sort.by(invalidSort));
    }

    @Test
    @DisplayName("Should check if user has admin role")
    void hasRole_WhenUserHasAdminRole_ShouldReturnTrue() {
        // Arrange
        String userId = "userId";
        Role adminRole = Role.ADMIN;

        // Set up mocks
        when(userRoleRepository.existsByUserUserIdAndRole(userId, adminRole)).thenReturn(true);

        // Act
        boolean result = userService.hasRole(userId, adminRole);

        // Assert
        assertThat(result).isTrue();
        verify(userRoleRepository).existsByUserUserIdAndRole(userId, adminRole);
    }

    @Test
    @DisplayName("Should check if user has user role")
    void hasRole_WhenUserHasUserRole_ShouldReturnTrue() {
        // Arrange
        String userId = "userId";
        Role userRole = Role.USER;

        // Set up mocks
        when(userRoleRepository.existsByUserUserIdAndRole(userId, userRole)).thenReturn(true);

        // Act
        boolean result = userService.hasRole(userId, userRole);

        // Assert
        assertThat(result).isTrue();
        verify(userRoleRepository).existsByUserUserIdAndRole(userId, userRole);
    }

    @Test
    @DisplayName("Should check if user does not have admin role")
    void hasRole_WhenUserDoesNotHaveAdminRole_ShouldReturnFalse() {
        // Arrange
        String userId = "userId";
        Role adminRole = Role.ADMIN;

        // Set up mocks
        when(userRoleRepository.existsByUserUserIdAndRole(userId, adminRole)).thenReturn(false);

        // Act
        boolean result = userService.hasRole(userId, adminRole);

        // Assert
        assertThat(result).isFalse();
        verify(userRoleRepository).existsByUserUserIdAndRole(userId, adminRole);
    }

    @Test
    @DisplayName("Should check if user does not have user role")
    void hasRole_WhenUserDoesNotHaveUserRole_ShouldReturnFalse() {
        // Arrange
        String userId = "userId";
        Role userRole = Role.USER;

        // Set up mocks
        when(userRoleRepository.existsByUserUserIdAndRole(userId, userRole)).thenReturn(false);

        // Act
        boolean result = userService.hasRole(userId, userRole);

        // Assert
        assertThat(result).isFalse();
        verify(userRoleRepository).existsByUserUserIdAndRole(userId, userRole);
    }

    @Test
    @DisplayName("Should remove role and call repository method")
    void removeRole_ShouldCallRepository() {
        // Arrange
        String userId = "userId";
        Role role = Role.ADMIN;

        User user = new User();
        user.setUserId(userId);
        user.setEmail("test@example.com");

        // Mock the user repository to return the user (needed for getUserById validation)
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        doNothing().when(userRoleRepository).deleteByUserUserIdAndRole(userId, role);

        // Act
        userService.removeRole(userId, role);

        // Assert & Verify
        verify(userRepository).findById(userId);
        verify(userRoleRepository).deleteByUserUserIdAndRole(userId, role);
    }

    @Test
    @DisplayName("Should check if admin exists when admin exists")
    void adminExists_WhenAdminExists_ShouldReturnTrue() {
        // Arrange
        when(userRoleRepository.existsByRole(Role.ADMIN)).thenReturn(true);

        // Act
        boolean result = userService.adminExists();

        // Assert
        assertThat(result).isTrue();

        // Verify
        verify(userRoleRepository).existsByRole(Role.ADMIN);
    }

    @Test
    @DisplayName("Should check if admin exists when no admin exists")
    void adminExists_WhenNoAdmin_ShouldReturnFalse() {
        // Arrange
        when(userRoleRepository.existsByRole(Role.ADMIN)).thenReturn(false);

        // Act
        boolean result = userService.adminExists();

        // Assert
        assertThat(result).isFalse();

        // Verify
        verify(userRoleRepository).existsByRole(Role.ADMIN);
    }

    @Test
    @DisplayName("Should get all users as DTOs with valid sort")
    void getAllUsersAsDto_WithValidSort_ShouldReturnUserDtos() {
        // Arrange
        String sort = "email";

        User user1 = new User();
        user1.setUserId("user1");
        user1.setEmail("user1@example.com");

        User user2 = new User();
        user2.setUserId("user2");
        user2.setEmail("user2@example.com");

        List<User> users = List.of(user1, user2);

        UserDto userDto1 = UserDto.builder()
                .userId("user1")
                .email("user1@example.com")
                .build();

        UserDto userDto2 = UserDto.builder()
                .userId("user2")
                .email("user2@example.com")
                .build();

        List<UserDto> expectedUserDtos = List.of(userDto1, userDto2);

        // Set up mocks
        when(userRepository.findAll(Sort.by(sort))).thenReturn(users);
        when(userMapper.toDto(user1)).thenReturn(userDto1);
        when(userMapper.toDto(user2)).thenReturn(userDto2);

        // Act
        List<UserDto> result = userService.getAllUsersAsDto(sort);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).isEqualTo(expectedUserDtos);

        // Verify
        verify(userRepository).findAll(Sort.by(sort));
        verify(userMapper).toDto(user1);
        verify(userMapper).toDto(user2);
    }

    @Test
    @DisplayName("Should get all users as DTOs with invalid sort and use default sort")
    void getAllUsersAsDto_WithInvalidSort_ShouldUseDefaultSort() {
        // Arrange
        String sort = "email";

        User user1 = new User();
        user1.setUserId("user1");
        user1.setEmail("user1@example.com");

        User user2 = new User();
        user2.setUserId("user2");
        user2.setEmail("user2@example.com");

        List<User> users = List.of(user1, user2);

        UserDto userDto1 = UserDto.builder()
                .userId("user1")
                .email("user1@example.com")
                .build();

        UserDto userDto2 = UserDto.builder()
                .userId("user2")
                .email("user2@example.com")
                .build();

        List<UserDto> expectedUserDtos = List.of(userDto1, userDto2);

        // Set up mocks
        when(userRepository.findAll(Sort.by("createdAt"))).thenReturn(users);
        when(userMapper.toDto(user1)).thenReturn(userDto1);
        when(userMapper.toDto(user2)).thenReturn(userDto2);

        // Act
        List<UserDto> result = userService.getAllUsersAsDto("invalidSort");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).isEqualTo(expectedUserDtos);

        // Verify
        verify(userRepository).findAll(Sort.by("createdAt"));
        verify(userMapper).toDto(user1);
        verify(userMapper).toDto(user2);
    }

    @Test
    @DisplayName("Should return empty list when no users exist for getAllUsersAsDto")
    void getAllUsersAsDto_WhenNoUsersExist_ShouldReturnEmptyList() {
        // Arrange
        String sort = "email";
        List<User> users = List.of();
        List<UserDto> expectedDtos = List.of();

        // Set up mocks
        when(userRepository.findAll(Sort.by(sort))).thenReturn(users);

        // Act
        List<UserDto> result = userService.getAllUsersAsDto(sort);

        // Assert
        assertThat(result).isNotNull().isEmpty();

        // Verify
        verify(userRepository).findAll(Sort.by(sort));
        verify(userMapper, never()).toDto(any());
    }

    @Test
    @DisplayName("Should return user dto when user exists")
    void getUserByEmailAsDto_WhenUserExists_ShouldReturnUserDto() {
        // Arrange
        String email = "test@example.com";
        User user = new User();
        user.setUserId("userId");
        user.setEmail(email);

        UserDto expectedDto = UserDto.builder()
                .userId("userId")
                .email(email)
                .build();

        // Set up mocks
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(expectedDto);

        // Act
        UserDto result = userService.getUserByEmailAsDto(email);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(expectedDto);

        // Verify
        verify(userRepository).findByEmail(email);
        verify(userMapper).toDto(user);
    }

    @Test
    @DisplayName("Should throw exception when user not found for getUserByEmailAsDto")
    void getUserByEmailAsDto_WhenUserNotFound_ShouldThrowException() {
        // Arrange
        String email = "nonExistentEmail";

        // Set up mock
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.getUserByEmailAsDto(email))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found");

        // Verify
        verify(userRepository).findByEmail(email);
        verify(userMapper, never()).toDto(any());
    }

    @Test
    @DisplayName("Should return user dto when user exists by ID")
    void getUserByIdAsDto_WhenUserExists_ShouldReturnUserDto() {
        // Arrange
        String userId = "userId";
        User user = new User();
        user.setUserId(userId);
        user.setEmail("test@example.com");

        UserDto expectedDto = UserDto.builder()
                .userId(userId)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        // Set up mocks
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(expectedDto);

        // Act
        UserDto result = userService.getUserByIdAsDto(userId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(expectedDto);

        // Verify
        verify(userRepository).findById(userId);
        verify(userMapper).toDto(user);
    }

    @Test
    @DisplayName("Should throw exception when user not found for getUserByIdAsDto")
    void getUserByIdAsDto_WhenUserNotFound_ShouldThrowException() {
        // Arrange
        String userId = "nonExistentUserId";

        // Set up mock
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.getUserByIdAsDto(userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found");

        // Verify
        verify(userRepository).findById(userId);
        verify(userMapper, never()).toDto(any());
    }
}