package com.lynus.cs203.config;

import com.lynus.cs203.entities.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Role Converter Unit Test")
public class RoleConverterTest {

    private RoleConverter roleConverter;

    @BeforeEach
    void setUp() {
        roleConverter = new RoleConverter();
    }

    @Test
    @DisplayName("Should convert Role to database column if valid")
    void convertToDatabaseColumn_WithValidRole_ShouldReturnsShortId() {
        // Given
        Role adminRole = Role.ADMIN;

        // When
        Short result = roleConverter.convertToDatabaseColumn(adminRole);

        // Then
        assertThat(result).isEqualTo((short) 2);
    }

    @Test
    @DisplayName("Should return null when converting null Role to database column")
    void convertToDatabaseColumn_WithNullRole_ShouldReturnsNull() {
        // When
        Short result = roleConverter.convertToDatabaseColumn(null);

        // Then
        assertThat(result).isNull();
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    @DisplayName("Should convert all enum values to database column correctly")
    void convertToDatabaseColumn_WithAllRoles_ShouldReturnsShort(Role role) {
        // When
        Short result = roleConverter.convertToDatabaseColumn(role);

        // Then
        assertThat(result).isEqualTo((short) role.getId());
    }

    @Test
    @DisplayName("Should convert Short to Role entity with valid id")
    void convertToEntityAttribute_WithValidShort_ShouldReturnsRole() {
        // Given
        Short adminId = (short) 2;

        // When
        Role result = roleConverter.convertToEntityAttribute(adminId);

        // Then
        assertThat(result).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("Should return null when converting null Short to Role entity")
    void convertToEntityAttribute_WithNullShort_ShouldReturnsNull() {
        // When
        Role result = roleConverter.convertToEntityAttribute(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should convert all valid role IDs to corresponding Role entities")
    void convertToEntityAttribute_WithAllValidId_ShouldReturnCorrectRoles() {
        // Given
        for (Role expectedRole: Role.values()) {
            Short roleId = (short) expectedRole.getId();

            // When
            Role result = roleConverter.convertToEntityAttribute(roleId);

            // Then
            assertThat(result).isEqualTo(expectedRole);
        }
    }

    @Test
    @DisplayName("Should handle invalid role ID by returning null")
    void convertToEntityAttribute_WithInvalidRole_ShouldReturnNull() {
        // Given
        Short invalidRoleId = (short) 99;

        // When
        assertThatThrownBy(() -> roleConverter.convertToEntityAttribute(invalidRoleId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No role found with id: 99");
    }

    @Test
    @DisplayName("Should handle negative ID values by returning null")
    void convertToEntityAttribute_WithNegativeId_ShouldReturnNull() {
        // Given
        Short negativeRoleId = (short) -1;

        // When
        assertThatThrownBy(() -> roleConverter.convertToEntityAttribute(negativeRoleId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No role found with id: -1");
    }

    @Test
    @DisplayName("Should handle zero ID value by returning null")
    void convertToEntityAttribute_WithZeroId_ShouldReturnNull() {
        // Given
        Short zeroRoleId = (short) 0;

        // When
        assertThatThrownBy(() -> roleConverter.convertToEntityAttribute(zeroRoleId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No role found with id: 0");
    }

    @Test
    @DisplayName("Role conversion should be consistent both ways")
    void roleConversion_ShouldReturnOriginalRole() {
        // Given
        Role originalRole = Role.USER;

        // When
        Short dbValue = roleConverter.convertToDatabaseColumn(originalRole);
        Role convertedRole = roleConverter.convertToEntityAttribute(dbValue);

        // Then
        assertThat(convertedRole).isEqualTo(originalRole);
    }

    @Test
    @DisplayName("Role conversion with null should return null")
    void roleConversion_WithNull_ShouldReturnNull() {
        // When
        Short dbValue = roleConverter.convertToDatabaseColumn(null);
        Role convertedRole = roleConverter.convertToEntityAttribute(dbValue);

        // Then
        assertThat(convertedRole).isNull();
    }

    @Test
    @DisplayName("Should handle maximum Short value gracefully")
    void convertToEntityAttribute_WithMaxShort_ShouldReturnNull() {
        // Given
        Short maxShortValue = Short.MAX_VALUE;

        // When
        assertThatThrownBy(() -> roleConverter.convertToEntityAttribute(maxShortValue))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No role found with id: 32767");
    }

    @Test
    @DisplayName("Should handle minimum Short value gracefully")
    void convertToEntityAttribute_WithMinShort_ShouldReturnNull() {
        // Given
        Short minShortValue = Short.MIN_VALUE;

        // When
        assertThatThrownBy(() -> roleConverter.convertToEntityAttribute(minShortValue))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No role found with id: -32768");
    }

}
