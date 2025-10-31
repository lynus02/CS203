package com.lynus.cs203.controllers;

import com.lynus.cs203.dtos.response.RoleOperationResponse;
import com.lynus.cs203.dtos.response.UserDto;
import com.lynus.cs203.dtos.response.UserRolesResponse;
import com.lynus.cs203.services.AdminService;
import com.lynus.cs203.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminController Unit Tests")
public class AdminControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private AdminService adminService;

    @InjectMocks
    private AdminController adminController;

    private UserDto testuser1;
    private UserDto testuser2;
    private UserRolesResponse userRolesResponse;
    private RoleOperationResponse roleOperationResponse;

    @BeforeEach
    void setUp() {
        testuser1 = UserDto.builder()
                .userId("user1")
                .email("johndoe@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        testuser2 = UserDto.builder()
                .userId("user2")
                .email("janedoe@example.com")
                .firstName("Jane")
                .lastName("Doe")
                .build();

        userRolesResponse = UserRolesResponse.builder()
                .userId("user1")
                .roles(List.of("USER", "ADMIN"))
                .build();

        roleOperationResponse = RoleOperationResponse.builder()
                .message("Role assigned successfully")
                .userId("user1")
                .role("ADMIN")
                .build();
    }

    @Test
    @DisplayName("Should return all users with default sort")
    void getAllUsers_WithDefaultSort_ShouldReturnUsers() {
        // Arrange
        List<UserDto> expectedUsers = List.of(testuser2, testuser1);
        when(userService.getAllUsersAsDto("name")).thenReturn(expectedUsers);

        // Act
        ResponseEntity<List<UserDto>> response = adminController.getAllUsers("name");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedUsers);
        assertThat(response.getBody()).hasSize(2);

        // Verify
        verify(userService).getAllUsersAsDto("name");
    }

    @Test
    @DisplayName("Should return all users with email sort")
    void getAllUsers_WithEmailSort_ShouldReturnUsers() {
        // Arrange
        List<UserDto> expectedUsers = List.of(testuser1, testuser2);
        when(userService.getAllUsersAsDto("email")).thenReturn(expectedUsers);

        // Act
        ResponseEntity<List<UserDto>> response = adminController.getAllUsers("email");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedUsers);
        assertThat(response.getBody()).hasSize(2);

        // Verify
        verify(userService).getAllUsersAsDto("email");
    }

    @Test
    @DisplayName("Should return empty list when no users exist")
    void getAllUsers_NoUsersExist_ShouldReturnEmptyList() {
        // Arrange
        when(userService.getAllUsersAsDto("name")).thenReturn(List.of());

        // Act
        ResponseEntity<List<UserDto>> response = adminController.getAllUsers("name");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();

        // Verify
        verify(userService).getAllUsersAsDto("name");
    }

    @Test
    @DisplayName("Should return user by ID")
    void getUser_WithValidUserId_ShouldReturnUser() {
        // Arrange
        String userId = "user1";
        when(userService.getUserByIdAsDto(userId)).thenReturn(testuser1);

        // Act
        ResponseEntity<UserDto> response = adminController.getUser(userId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(testuser1);
        assertThat(response.getBody().getUserId()).isEqualTo(userId);

        // Verify
        verify(userService).getUserByIdAsDto(userId);
    }

    @Test
    @DisplayName("Should handle different user IDs")
    void getUser_WithDifferentUserIds_ShouldReturnCorrectUsers() {
        // Arrange
        String userId1 = "user1";
        String userId2 = "user2";
        when(userService.getUserByIdAsDto(userId1)).thenReturn(testuser1);
        when(userService.getUserByIdAsDto(userId2)).thenReturn(testuser2);

        // Act
        ResponseEntity<UserDto> response1 = adminController.getUser(userId1);
        ResponseEntity<UserDto> response2 = adminController.getUser(userId2);

        // Assert for user1
        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response1.getBody()).isEqualTo(testuser1);
        assertThat(response1.getBody().getUserId()).isEqualTo(userId1);

        // Assert for user2
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response2.getBody()).isEqualTo(testuser2);
        assertThat(response2.getBody().getUserId()).isEqualTo(userId2);

        // Verify
        verify(userService).getUserByIdAsDto(userId1);
        verify(userService).getUserByIdAsDto(userId2);
    }

    @Test
    @DisplayName("Should assign role to user")
    void assignRole_ShouldAssignRoleToUser() {
        // Arrange
        String userId = "user1";
        String roleName = "ADMIN";
        when(adminService.assignRoleToUser(userId, roleName)).thenReturn(roleOperationResponse);

        // Act
        ResponseEntity<RoleOperationResponse> response = adminController.assignRole(userId, roleName);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(roleOperationResponse);
        assertThat(response.getBody().getMessage()).contains("assigned");
        assertThat(response.getBody().getUserId()).isEqualTo(userId);
        assertThat(response.getBody().getRole()).isEqualTo(roleName);

        // Verify
        verify(adminService).assignRoleToUser(userId, roleName);
    }

    @Test
    @DisplayName("Should assign different roles")
    void assignRole_ShouldAssignDifferentRolesT() {
        // Arrange
        String userId = "user1";
        String adminRole = "ADMIN";
        String userRole = "USER";

        RoleOperationResponse response1 = RoleOperationResponse.builder()
                .message("Admin role assigned successfully")
                .userId(userId)
                .role(adminRole)
                .build();

        RoleOperationResponse response2 = RoleOperationResponse.builder()
                .message("User role assigned successfully")
                .userId(userId)
                .role(userRole)
                .build();

        when(adminService.assignRoleToUser(userId, adminRole)).thenReturn(response1);
        when(adminService.assignRoleToUser(userId, userRole)).thenReturn(response2);

        // Act
        ResponseEntity<RoleOperationResponse> result1 = adminController.assignRole(userId, adminRole);
        ResponseEntity<RoleOperationResponse> result2 = adminController.assignRole(userId, userRole);

        // Assert for roleName1
        assertThat(result1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result1.getBody()).isEqualTo(response1);
        assertThat(result1.getBody().getRole()).isEqualTo(adminRole);

        // Assert for roleName2
        assertThat(result2.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result2.getBody()).isEqualTo(response2);
        assertThat(result2.getBody().getRole()).isEqualTo(userRole);

        // Verify
        verify(adminService).assignRoleToUser(userId, adminRole);
        verify(adminService).assignRoleToUser(userId, userRole);
    }

    @Test
    @DisplayName("Should remove role from user")
    void removeRole_ShouldRemoveRoleFromUser() {
        // Arrange
        String userId = "user1";
        String roleName = "ADMIN";
        RoleOperationResponse removeRoleResponse = RoleOperationResponse.builder()
                .message("Role removed successfully")
                .userId(userId)
                .role(roleName)
                .build();

        when(adminService.removeRoleFromUser(userId, roleName)).thenReturn(removeRoleResponse);

        // Act
        ResponseEntity<RoleOperationResponse> response = adminController.removeRole(userId, roleName);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(removeRoleResponse);
        assertThat(response.getBody().getMessage()).contains("removed");

        // Verify
        verify(adminService).removeRoleFromUser(userId, roleName);
    }

    @Test
    @DisplayName("Should remove different roles")
    void removeRole_ShouldRemoveDifferentRoles() {
        // Arrange
        String userId = "user1";
        String adminRole = "ADMIN";
        String userRole = "USER";

        RoleOperationResponse response1 = RoleOperationResponse.builder()
                .message("Admin role removed successfully")
                .userId(userId)
                .role(adminRole)
                .build();

        RoleOperationResponse response2 = RoleOperationResponse.builder()
                .message("User role removed successfully")
                .userId(userId)
                .role(userRole)
                .build();

        when(adminService.removeRoleFromUser(userId, adminRole)).thenReturn(response1);
        when(adminService.removeRoleFromUser(userId, userRole)).thenReturn(response2);

        // Act
        ResponseEntity<RoleOperationResponse> result1 = adminController.removeRole(userId, adminRole);
        ResponseEntity<RoleOperationResponse> result2 = adminController.removeRole(userId, userRole);

        // Assert for roleName1
        assertThat(result1.getBody().getRole()).isEqualTo(adminRole);

        // Assert for roleName2
        assertThat(result2.getBody().getRole()).isEqualTo(userRole);

        // Verify
        verify(adminService).removeRoleFromUser(userId, adminRole);
        verify(adminService).removeRoleFromUser(userId, userRole);
    }

    @Test
    @DisplayName("Should return user roles")
    void getUserRoles_ShouldReturnUserRoles() {
        // Arrange
        String userId = "user1";
        when(adminService.getUserRoles(userId)).thenReturn(userRolesResponse);

        // Act
        ResponseEntity<UserRolesResponse> response = adminController.getUserRoles(userId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(userRolesResponse);
        assertThat(response.getBody().getUserId()).isEqualTo(userId);
        assertThat(response.getBody().getRoles()).containsExactlyInAnyOrder("USER", "ADMIN");

        // Verify
        verify(adminService).getUserRoles(userId);
    }

    @Test
    @DisplayName("Should return empty roles list when user has no roles")
    void getUserRoles_UserHasNoRoles_ShouldReturnEmptyList() {
        // Arrange
        String userId = "user3";
        UserRolesResponse emptyRolesResponse = UserRolesResponse.builder()
                .userId(userId)
                .roles(List.of())
                .build();

        when(adminService.getUserRoles(userId)).thenReturn(emptyRolesResponse);

        // Act
        ResponseEntity<UserRolesResponse> response = adminController.getUserRoles(userId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getRoles()).isEmpty();

        // Verify
        verify(adminService).getUserRoles(userId);
    }
}
