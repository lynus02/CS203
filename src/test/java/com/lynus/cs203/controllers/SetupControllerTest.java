package com.lynus.cs203.controllers;

import com.lynus.cs203.dtos.request.CreateUserRequest;
import com.lynus.cs203.dtos.response.AdminCreationResponse;
import com.lynus.cs203.dtos.response.SetupStatusResponse;
import com.lynus.cs203.exceptions.AdminAlreadyExistsException;
import com.lynus.cs203.services.SetupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Setup Controller Unit Tests")
public class SetupControllerTest {

    @Mock
    private SetupService setupService;

    @InjectMocks
    private SetupController setupController;

    @Test
    @DisplayName("Should create first admin user successfully")
    void createFirstAdmin_WhenValidRequest_ShouldReturnSuccessResponse() throws Exception {
        // Arrange
        CreateUserRequest request = CreateUserRequest.builder()
                .email("admin@example.com")
                .password("Password@123!")
                .firstName("Admin")
                .lastName("User")
                .build();

        AdminCreationResponse serviceResponse = AdminCreationResponse.builder()
                .message("First admin user created successfully")
                .userId("userId")
                .email("admin@example.com")
                .build();

        when(setupService.createFirstAdmin(any(CreateUserRequest.class)))
                .thenReturn(serviceResponse);

        // Act
        ResponseEntity<AdminCreationResponse> response = setupController.createFirstAdmin(request);

        // Act & Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getMessage()).isEqualTo("First admin user created successfully");
        assertThat(response.getBody().getUserId()).isEqualTo("userId");
        assertThat(response.getBody().getEmail()).isEqualTo("admin@example.com");

        // Verify
        verify(setupService).createFirstAdmin(any(CreateUserRequest.class));
    }

    @Test
    @DisplayName("Should return AdminAlreadyExists exception")
    void createFirstAdmin_WhenAdminAlreadyExists_ShouldReturnBadRequest() throws Exception {
        // Arrange
        CreateUserRequest request = CreateUserRequest.builder()
                .email("admin@example.com")
                .password("Password@123")
                .firstName("Admin")
                .lastName("User")
                .build();

        when(setupService.createFirstAdmin(any(CreateUserRequest.class)))
                .thenThrow(new AdminAlreadyExistsException("Admin setup already completed"));

        assertThatThrownBy(() -> setupController.createFirstAdmin(request))
                .isInstanceOf(AdminAlreadyExistsException.class)
                .hasMessage("Admin setup already completed");

        // Verify
        verify(setupService).createFirstAdmin(any(CreateUserRequest.class));
    }

    @Test
    @DisplayName("Should handle service exception gracefully")
    void createFirstAdmin_WhenServiceThrowsException_ShouldReturnBadRequest() throws Exception {
        // Arrange
        CreateUserRequest request = CreateUserRequest.builder()
                .email("admin@example.com")
                .password("Password@123!")
                .firstName("Admin")
                .lastName("User")
                .build();

        when(setupService.createFirstAdmin(any(CreateUserRequest.class)))
                .thenThrow(new RuntimeException("Database error"));

        assertThatThrownBy(() -> setupController.createFirstAdmin(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database error");

        // Verify
        verify(setupService).createFirstAdmin(request);
    }

    @Test
    @DisplayName("Should return setup status successfully")
    void getSetupStatus_ShouldReturnSetupStatus() throws Exception {
        // Arrange
        SetupStatusResponse response = SetupStatusResponse.builder()
                .setupComplete(true)
                .message("Admin user exists")
                .build();

        when(setupService.getSetupStatus()).thenReturn(response);

        ResponseEntity<SetupStatusResponse> result = setupController.getSetupStatus();

        // Act & Assert
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().isSetupComplete()).isTrue();
        assertThat(result.getBody().getMessage()).isEqualTo("Admin user exists");

        // Verify
        verify(setupService).getSetupStatus();
    }

    @Test
    @DisplayName("Should return service exception gracefully")
    void getSetupStatus_WhenServiceThrowsException_ShouldReturnInternalServerError() throws Exception {
        // Arrange
        when(setupService.getSetupStatus())
                .thenThrow(new RuntimeException("Service error"));

        // Act & Assert
        assertThatThrownBy(() -> setupController.getSetupStatus())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Service error");

        // Verify
        verify(setupService).getSetupStatus();
    }
}
