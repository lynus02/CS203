package com.lynus.cs203.controllers;

import com.lynus.cs203.dtos.request.ChangePasswordRequest;
import com.lynus.cs203.dtos.request.CreateUserRequest;
import com.lynus.cs203.dtos.request.UpdateUserRequest;
import com.lynus.cs203.dtos.response.PasswordChangeResponse;
import com.lynus.cs203.dtos.response.UserDto;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController Test")
public class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserController userController;

    private final String testUserId = "userId";
    private final String testEmail = "test@example.com";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    private void mockAuthentication() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(testUserId);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void getCurrentUserProfile_ShouldReturnUserDto_WhenUserIsAuthenticated() {
        // Arrange
        mockAuthentication();
        UserDto mockUserDto = createUserDto();
        when(userService.getUserByIdAsDto(testUserId)).thenReturn(mockUserDto);

        // Act
        ResponseEntity<UserDto> response = userController.getCurrentUserProfile();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(mockUserDto);
        verify(userService).getUserByIdAsDto(testUserId);
    }

    @Test
    void getCurrentUserProfile_ShouldExtractUserIdFromSecurityContext() {
        // Arrange
        String differentUserId = "differentUserId";
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(differentUserId);
        SecurityContextHolder.setContext(securityContext);

        UserDto mockUserDto = UserDto.builder()
                .userId(differentUserId)
                .email(testEmail)
                .firstName("John")
                .lastName("Doe")
                .build();

        when(userService.getUserByIdAsDto(differentUserId)).thenReturn(mockUserDto);

        // Act
        ResponseEntity<UserDto> response = userController.getCurrentUserProfile();

        // Assert
        assertThat(response.getBody().getUserId()).isEqualTo(differentUserId);
        verify(userService).getUserByIdAsDto(differentUserId);
    }

    @Test
    void createUser_ShouldReturnCreatedUserDto() {
        // Arrange
        CreateUserRequest request = createUserRequest();
        UserDto mockUserDto = createUserDto();
        when(userService.createUserAsDto(request)).thenReturn(mockUserDto);

        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromPath("http://localhost:8080");

        // Act
        ResponseEntity<UserDto> response = userController.createUser(request, uriComponentsBuilder);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(mockUserDto);
        verify(userService).createUserAsDto(request);
    }

    @Test
    void updateUser_ShouldReturnUpdatedUserDto() {
        // Arrange
        mockAuthentication();
        UpdateUserRequest request = updateUserRequest();
        UserDto mockUserDto = updateUserDto();
        when(userService.updateUserAsDto(testUserId, request)).thenReturn(mockUserDto);

        // Act
        ResponseEntity<UserDto> response = userController.updateCurrentUserProfile(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(mockUserDto);
        verify(userService).updateUserAsDto(testUserId, request);
    }

    @Test
    void getCurrentUserProfile_ShouldThrowIllegalStateException_WhenUnexpectedPrincipalType() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(authentication);

        // Create an unexpected principal
        Object unexpectedPrincipal = new Object();
        when(authentication.getPrincipal()).thenReturn(unexpectedPrincipal);
        SecurityContextHolder.setContext(securityContext);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> userController.getCurrentUserProfile());

        assertThat(exception.getMessage()).contains("Unexpected principal type");
    }

    // Helper Method
    private CreateUserRequest createUserRequest() {
        return CreateUserRequest.builder()
                .email(testEmail)
                .password("Password@123")
                .firstName("John")
                .lastName("Doe")
                .build();
    }

    private UpdateUserRequest updateUserRequest() {
        return UpdateUserRequest.builder()
                .firstName("UpdatedFirstName")
                .lastName("UpdatedLastName")
                .email(testEmail)
                .build();
    }

    private ChangePasswordRequest changePasswordRequest() {
        return ChangePasswordRequest.builder()
                .oldPassword("OldPassword@123")
                .newPassword("NewPassword@123")
                .build();
    }

    private UserDto createUserDto() {
        return UserDto.builder()
                .userId(testUserId)
                .email(testEmail)
                .firstName("John")
                .lastName("Doe")
                .build();
    }

    private UserDto updateUserDto() {
        return UserDto.builder()
                .userId(testUserId)
                .email("updatedemail@example.com")
                .firstName("UpdatedFirstName")
                .lastName("UpdatedLastName")
                .build();
    }

    private PasswordChangeResponse changePasswordResponse() {
        return PasswordChangeResponse.builder()
                .message("Password changed successfully")
                .build();
    }
}
