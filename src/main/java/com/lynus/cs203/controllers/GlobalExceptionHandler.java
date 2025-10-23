package com.lynus.cs203.controllers;

import com.lynus.cs203.dtos.response.ErrorResponse;
import com.lynus.cs203.exceptions.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException e
    ) {
        log.warn("Validation error occurred: {}", e.getMessage());

        var errors = new HashMap<String, String>();

        e.getBindingResult().getFieldErrors().forEach(error -> {
            errors.put(error.getField(), error.getDefaultMessage());
            log.debug("Validation error - Field: {}, Message: {}", error.getField(), error.getDefaultMessage());
        });

        var errorResponse = ErrorResponse.builder()
                .status (HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Validation errors in request")
                .errors(errors)
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(
            UserNotFoundException e
    ) {
        log.warn("User not found: {}", e.getMessage());

        var errorResponse = ErrorResponse.builder()
                .status (HttpStatus.NOT_FOUND.value())
                .error("User Not Found")
                .message(e.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(
            EmailAlreadyExistsException e
    ) {
        log.warn("Email already exists: {}", e.getMessage());

        var errors = Map.of("email", "Email already exists");

        var errorResponse = ErrorResponse.builder()
                .status (HttpStatus.CONFLICT.value())
                .error("Email Conflict")
                .message(e.getMessage())
                .errors(errors)
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPassword(
            InvalidPasswordException e
    ) {
        log.warn("Invalid password attempt: {}", e.getMessage());

        var errorResponse = ErrorResponse.builder()
                .status (HttpStatus.UNAUTHORIZED.value())
                .error("Invalid Password")
                .message("Provided password is incorrect")
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(InvalidRoleException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRoleException(
            InvalidRoleException e
    ) {
        log.warn("Invalid role provided: {}", e.getMessage());

        var errors = Map.of("role", "Invalid role name provided");

        var errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Invalid Role")
                .message(e.getMessage())
                .errors(errors)
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(AdminAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleAdminAlreadyExistsException(
            AdminAlreadyExistsException e
    ) {
        log.warn("Admin already exists: {}", e.getMessage());

        var errors = Map.of("admin", "Admin setup already completed");

        var errorResponse = ErrorResponse.builder()
                .status(HttpStatus.FORBIDDEN.value())
                .error("Admin Already Exists")
                .message(e.getMessage())
                .errors(errors)
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException e
    ) {
        log.warn("Authentication failed: {}", e.getMessage());

        var errorResponse = ErrorResponse.builder()
                .status (HttpStatus.UNAUTHORIZED.value())
                .error("Authentication Failed")
                .message("Invalid username or password")
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException e
    ) {
        log.warn("Access denied: {}", e.getMessage());

        var errorResponse = ErrorResponse.builder()
                .status (HttpStatus.FORBIDDEN.value())
                .error("Access Denied")
                .message("You do not have permission to access this resource")
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException e
    ) {
        log.error("Database constraint violation: {}", e.getMessage(), e);

        var errorResponse = ErrorResponse.builder()
                .status (HttpStatus.CONFLICT.value())
                .error("Data Conflict")
                .message("Database constraint violated")
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException e
    ) {
        log.warn("Invalid argument provided: {}", e.getMessage());

        var errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Invalid Input")
                .message(e.getMessage())
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(IncorrectResultSizeDataAccessException.class)
    public ResponseEntity<ErrorResponse> handleIncorrectResultSizeDataAccess(
            IncorrectResultSizeDataAccessException e
    ) {
        log.error("Database query returned multiple results when expecting single result: {}", e.getMessage());

        var errorResponse = ErrorResponse.builder()
                .status(HttpStatus.CONFLICT.value())
                .error("Data Integrity Issue")
                .message("Multiple records found where only one was expected. Please contact support.")
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(
            Exception e
    ) {
        log.error("Unhandled exception occurred: {}", e.getMessage(), e);

        var errorResponse = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred. Please try again later.")
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

}
