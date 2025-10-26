package com.lynus.cs203.services;

import com.lynus.cs203.entities.Role;
import com.lynus.cs203.exceptions.InvalidRoleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class RoleValidationServiceTest {

    @InjectMocks
    private RoleValidationService roleValidationService;

    @Test
    @DisplayName("Should return role when valid role name is provided")
    void validateAndGetRole_WhenValidRoleName_ShouldReturnRole() {
        // Arrange
        String roleName = "admin";

        // Act
        Role result = roleValidationService.validateAndGetRole(roleName);

        // Assert
        assertThat(result).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("Should return role when valid role when valid role is uppercase")
    void validateAndGetRole_WhenValidRoleUppercase_ShouldReturnRole() {
        // Arrange
        String roleName = "ADMIN";

        // Act
        Role result = roleValidationService.validateAndGetRole(roleName);

        // Assert
        assertThat(result).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("Should return role when valid role when valid role is mixed case")
    void validateAndGetRole_WhenValidRoleMixedCase_ShouldReturnRole() {
        // Arrange
        String roleName = "AdmIN";

        // Act
        Role result = roleValidationService.validateAndGetRole(roleName);

        // Assert
        assertThat(result).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("Should throw InvalidRoleException when invalid role name is provided")
    void validateAndGetRole_WhenInvalidRoleName_ShouldThrowInvalidRoleException() {
        // Arrange
        String invalidRoleName = "invalid";

        // Act & Assert
        assertThatThrownBy(() -> roleValidationService.validateAndGetRole(invalidRoleName))
                .isInstanceOf(InvalidRoleException.class)
                .hasMessageContaining("Invalid role name");
    }
}
