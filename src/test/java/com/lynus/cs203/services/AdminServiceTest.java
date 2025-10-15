package com.lynus.cs203.services;

import com.lynus.cs203.dtos.response.RoleOperationResponse;
import com.lynus.cs203.dtos.response.UserRolesResponse;
import com.lynus.cs203.entities.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AdminServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private RoleValidationService roleValidationService;

    @InjectMocks
    private AdminService adminService;

    @Test
    void assignRoleToUser_WhenValidInput_ShouldAssignRole() {
        // Arrange
        String userId = "userId";
        String roleName = "ADMIN";

        Role mockRole = Role.ADMIN;

        // Set up mocks
        when(roleValidationService.validateAndGetRole(roleName)).thenReturn(mockRole);

        // Act
        RoleOperationResponse response = adminService.assignRoleToUser(userId, roleName);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("Role assigned successfully");
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getRole()).isEqualTo(roleName.toUpperCase());

        // Verify
        verify(roleValidationService).validateAndGetRole(roleName);
        verify(userService).assignRole(userId, mockRole);
    }

    @Test
    void removeRoleFromUser_WhenValidInput_ShouldRemoveRole() {
        String userId = "userId";
        String roleName = "ADMIN";
        Role mockRole = Role.ADMIN;

        // Set up mocks
        when(roleValidationService.validateAndGetRole(roleName)).thenReturn(mockRole);

        // Act
        RoleOperationResponse response = adminService.removeRoleFromUser(userId, roleName);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("Role removed successfully");
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getRole()).isEqualTo(roleName.toUpperCase());

        // Verify
        verify(roleValidationService).validateAndGetRole(roleName);
        verify(userService).removeRole(userId, mockRole);
    }

    @Test
    void getUserRoles_WhenValidInput_ShouldReturnRoles() {
        String userId = "userId";
        List<String> mockRoles = List.of("ADMIN", "USER");

        // Set up mocks
        when(userService.getUserRoles(userId)).thenReturn(mockRoles);

        // Act
        UserRolesResponse response = adminService.getUserRoles(userId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getRoles()).isEqualTo(mockRoles);

        // Verify
        verify(userService).getUserRoles(userId);
    }
}
