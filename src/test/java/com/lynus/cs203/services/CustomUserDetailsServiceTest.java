package com.lynus.cs203.services;

import com.lynus.cs203.entities.Role;
import com.lynus.cs203.entities.User;
import com.lynus.cs203.entities.UserRole;
import com.lynus.cs203.entities.UserRoleId;
import com.lynus.cs203.repositories.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Custom User Details Service Unit Test")
public class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("Should return user details when user exists")
    void loadUserByUsername_WhenUserExists_ReturnsUserDetails() {
        // Arrange
        String email = "test@example.com";
        String userId = "userId";
        String password = "Password@123";

        User user = new User();
        user.setUserId(userId);
        user.setEmail(email);
        user.setPassword(password);
        user.setIsActive(true);

        // Create UserRoles
        Set<UserRole> userRoles = new HashSet<>();

        UserRole userRole = new UserRole();
        UserRoleId userRoleId = new UserRoleId();
        userRoleId.setUserId(userId);
        userRoleId.setRoleId((short) Role.USER.getId());
        userRole.setId(userRoleId);
        userRole.setRole(Role.USER);
        userRoles.add(userRole);

        UserRole adminRole = new UserRole();
        UserRoleId adminRoleId = new UserRoleId();
        adminRoleId.setUserId(userId);
        adminRoleId.setRoleId((short) Role.ADMIN.getId());
        adminRole.setId(adminRoleId);
        adminRole.setRole(Role.ADMIN);
        userRoles.add(adminRole);

        user.setUserRoles(userRoles);

        when(userService.getUserRoles(userId)).thenReturn(List.of("USER", "ADMIN"));
        when(userRepository.findByEmailWithProfile(email)).thenReturn(Optional.of(user));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        // Assert
        assertThat(userDetails.getUsername()).isEqualTo(userId);
        assertThat(userDetails.getPassword()).isEqualTo(password);
        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        assertThat(userDetails.isEnabled()).isTrue();

        // Verify
        verify(userRepository).findByEmailWithProfile(email);
        verify(userService).getUserRoles(userId);
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException when user does not exist")
    void loadUserByUsername_WhenUserDoesNotExist_ThrowsException() {
        // Arrange
        String email = "nonexistent@example.com";

        when(userRepository.findByEmailWithProfile(email)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(email))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found with email");

        // Verify
        verify(userRepository).findByEmailWithProfile(email);
        verify(userService, never()).getUserRoles(anyString());
    }

    @Test
    @DisplayName("Should return empty authorities when user has no roles")
    void loadUserByUsername_WhenUserHasNoRoles_ReturnsEmptyAuthorities() {
        // Arrange
        String userId = "userId";
        String email = "test@example.com";
        User user = new User();
        user.setUserId(userId);
        user.setEmail(email);
        user.setPassword("Password@123");
        user.setIsActive(true);
        user.setUserRoles(new HashSet<>()); // Empty roles

        when(userService.getUserRoles(userId)).thenReturn(List.of());
        when(userRepository.findByEmailWithProfile(email)).thenReturn(Optional.of(user));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getAuthorities()).isEmpty();
        assertThat(userDetails.isEnabled()).isTrue();

        // Verify
        verify(userRepository).findByEmailWithProfile(email);
        verify(userService).getUserRoles(userId);
    }

    @Test
    @DisplayName("Should return disabled user when user is inactive")
    void loadUserByUsername_WhenUserIsInactive_ReturnsDisabledUser() {
        // Arrange
        String email = "inactive@example.com";
        String userId = "inactiveUserId";
        String password = "Password@123";

        User user = new User();
        user.setUserId(userId);
        user.setEmail(email);
        user.setPassword(password);
        user.setIsActive(false); // Inactive user
        user.setUserRoles(new HashSet<>());

        when(userService.getUserRoles(userId)).thenReturn(List.of());
        when(userRepository.findByEmailWithProfile(email)).thenReturn(Optional.of(user));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(userId);
        assertThat(userDetails.isEnabled()).isFalse(); // Should be disabled
        assertThat(userDetails.getAuthorities()).isEmpty();

        // Verify
        verify(userRepository).findByEmailWithProfile(email);
        verify(userService).getUserRoles(userId);
    }
}