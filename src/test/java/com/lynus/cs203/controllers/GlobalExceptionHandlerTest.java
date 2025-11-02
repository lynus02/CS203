package com.lynus.cs203.controllers;

import com.lynus.cs203.exceptions.UnauthorizedException;
import com.lynus.cs203.dtos.response.ErrorResponse;
import com.lynus.cs203.exceptions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonParseException;

import java.nio.file.AccessDeniedException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler Unit Test")
public class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Test
    @DisplayName("Should handle MethodArgumentNotValidException")
    void handleMethodArgumentNotValidException() {
        // Arrange
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        FieldError fieldError1 = new FieldError(
                "object", "email", "Email is required");
        FieldError fieldError2 = new FieldError(
                "object", "password", "Password must be at least 8 characters");
        List<FieldError> fieldErrors = List.of(fieldError1, fieldError2);

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(fieldErrors);

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleValidationErrors(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getBody().getError()).isEqualTo("Validation Failed");
        assertThat(response.getBody().getErrors())
                .containsEntry("email", "Email is required");
        assertThat(response.getBody().getErrors())
                .containsEntry("password", "Password must be at least 8 characters");
    }

    @Test
    @DisplayName("Should handle UserNotFoundException")
    void handleUserNotFoundException() {
        // Arrange
        UserNotFoundException exception = new
                UserNotFoundException("User not found with id: 123");

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleUserNotFoundException(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(response.getBody().getError()).isEqualTo("User Not Found");
        assertThat(response.getBody().getMessage()).isEqualTo("User not found with id: 123");
    }

    @Test
    @DisplayName("Should handle EmailAlreadyExistsException")
    void handleEmailAlreadyExistsException() {
        // Arrange
        EmailAlreadyExistsException exception = new
                EmailAlreadyExistsException("Email already exists: test@example.com");

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleEmailAlreadyExists(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(response.getBody().getError()).isEqualTo("Email Conflict");
        assertThat(response.getBody().getMessage())
                .isEqualTo("Email already exists: test@example.com");
        assertThat(response.getBody().getErrors())
                .containsEntry("email", "Email already exists");
    }

    @Test
    @DisplayName("Should handle InvalidPasswordException")
    void handleInvalidPasswordException() {
        // Arrange
        InvalidPasswordException exception = new
                InvalidPasswordException("Invalid password");

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleInvalidPassword(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getBody().getError()).isEqualTo("Invalid Password");
        assertThat(response.getBody().getMessage()).isEqualTo("Provided password is incorrect");
    }

    @Test
    @DisplayName("Should handle InvalidRoleException")
    void handleInvalidRoleException() {
        // Arrange
        InvalidRoleException exception = new
                InvalidRoleException("Invalid role: SUPERVISOR");

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleInvalidRoleException(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getBody().getError()).isEqualTo("Invalid Role");
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid role: SUPERVISOR");
        assertThat(response.getBody().getErrors())
                .containsEntry("role", "Invalid role name provided");
    }

    @Test
    @DisplayName("Should handle AdminAlreadyExistsException")
    void handleAdminAlreadyExistsException() {
        // Arrange
        AdminAlreadyExistsException exception = new
                AdminAlreadyExistsException("Admin already exists");

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleAdminAlreadyExistsException(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getBody().getError()).isEqualTo("Admin Already Exists");
        assertThat(response.getBody().getMessage()).isEqualTo("Admin already exists");
        assertThat(response.getBody().getErrors())
                .containsEntry("admin", "Admin setup already completed");
    }

    @Test
    @DisplayName("Should handle HttpMessageNotReadableException")
    void handleHttpMessageNotReadableException() {
        // Arrange
        HttpMessageNotReadableException exception = new
                HttpMessageNotReadableException("Invalid JSON format",
                new RuntimeException("JSON parse error"));

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleJsonParseException(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getBody().getError()).isEqualTo("Malformed JSON");
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid JSON format");
    }

    @Test
    @DisplayName("Should handle JsonParseException")
    void handleJsonParseException() {
        // Arrange
        JsonParseException exception = new
                JsonParseException(null, "Invalid JSON format");

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleJsonParseException(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getBody().getError()).isEqualTo("Malformed JSON");
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid JSON format");
    }

    @Test
    @DisplayName("Should handle BadCredentialsException")
    void handleBadCredentialsException() {
        // Arrange
        BadCredentialsException exception = new
                BadCredentialsException("Bad credentials");

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleBadCredentials(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getBody().getError()).isEqualTo("Authentication Failed");
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid username or password");
    }

    @Test
    @DisplayName("Should handle AccessDeniedException")
    void handleAccessDeniedException() {
        // Arrange
        AccessDeniedException exception = new
                AccessDeniedException("Access denied");

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleAccessDenied(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getBody().getError()).isEqualTo("Access Denied");
        assertThat(response.getBody().getMessage())
                .isEqualTo("You do not have permission to access this resource");
    }

    @Test
    @DisplayName("Should handle DataIntegrityViolationException")
    void handleDataIntegrityViolationException() {
        // Arrange
        DataIntegrityViolationException exception = new
                DataIntegrityViolationException("Constraint violation");

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleDataIntegrityViolation(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(response.getBody().getError()).isEqualTo("Data Conflict");
        assertThat(response.getBody().getMessage()).isEqualTo("Database constraint violated");
    }

    @Test
    @DisplayName("Should handle IllegalArgumentException")
    void handleIllegalArgumentException() {
        // Arrange
        IllegalArgumentException exception = new
                IllegalArgumentException("Illegal argument provided");

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleIllegalArgumentException(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getBody().getError()).isEqualTo("Invalid Input");
        assertThat(response.getBody().getMessage()).isEqualTo("Illegal argument provided");
    }

    @Test
    @DisplayName("Should handle IncorrectResultSizeDataAccessException")
    void handleIncorrectResultSizeDataAccessException() {
        // Arrange
        IncorrectResultSizeDataAccessException exception = new
                IncorrectResultSizeDataAccessException(1, 2);

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleIncorrectResultSizeDataAccess(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(response.getBody().getError()).isEqualTo("Data Integrity Issue");
        assertThat(response.getBody().getMessage())
                .contains("Multiple records found");
    }

    @Test
    @DisplayName("Should handle UnauthorizedException")
    void handleUnauthorizedException() {
        // Arrange
        UnauthorizedException exception = new
                UnauthorizedException("User is unauthorized");

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleUnauthorizedException(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getBody().getError()).isEqualTo("User is unauthorized");
        assertThat(response.getBody().getMessage())
                .isEqualTo("User is unauthorized");
    }

    @Test
    @DisplayName("Should handle generic Exception")
    void handleGenericException() {
        // Arrange
        Exception exception = new
                Exception("Something went wrong");

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleGeneral(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().getMessage())
                .isEqualTo("An unexpected error occurred. Please try again later.");
    }

    @Test
    @DisplayName("Should return consistent Error Response structure")
    void allExceptionHandlers_ShouldReturnConsistentErrorResponseStructure() {
        // Test different exceptions to ensure consistent response structure
        Exception[] exceptions = {
                new UserNotFoundException("Test"),
                new EmailAlreadyExistsException("Test"),
                new InvalidRoleException("Test")
        };

        for (Exception exception : exceptions) {
            ResponseEntity<ErrorResponse> response = null;

            if (exception instanceof UserNotFoundException) {
                response = globalExceptionHandler.handleUserNotFoundException((UserNotFoundException) exception);
            } else if (exception instanceof EmailAlreadyExistsException) {
                response = globalExceptionHandler.handleEmailAlreadyExists((EmailAlreadyExistsException) exception);
            } else if (exception instanceof InvalidRoleException) {
                response = globalExceptionHandler.handleInvalidRoleException((InvalidRoleException) exception);
            }

            assertThat(response).isNotNull();
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isPositive();
            assertThat(response.getBody().getError()).isNotBlank();
            assertThat(response.getBody().getMessage()).isNotBlank();
        }
    }
}
