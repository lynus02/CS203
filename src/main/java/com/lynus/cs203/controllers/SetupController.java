package com.lynus.cs203.controllers;

import com.lynus.cs203.dtos.request.CreateUserRequest;
import com.lynus.cs203.dtos.response.AdminCreationResponse;
import com.lynus.cs203.dtos.response.SetupStatusResponse;
import com.lynus.cs203.services.SetupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "System Setup", description = "System initialization and setup operations")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/setup")
public class SetupController {
    private final SetupService setupService;

    @Operation(
            summary = "Create first admin user",
            description = "Initialize the system by creating the first admin user. This endpoint is only available when no admin users exist in the system."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "First admin user created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "409", description = "Admin user already exists")
    })
    @PostMapping("/admin")
    public ResponseEntity<AdminCreationResponse> createFirstAdmin(
            @Valid @RequestBody CreateUserRequest request
    ) {
        log.info("Creating first admin user for email: {}", request.getEmail());

        AdminCreationResponse response = setupService.createFirstAdmin(request);

        log.info("Successfully created first admin user: {}", request.getEmail());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get system setup status",
            description = "Check if the system has been initialized (i.e., whether an admin user exists)"
    )
    @ApiResponse(responseCode = "200", description = "Setup status retrieved successfully")
    @GetMapping("/status")
    public ResponseEntity<SetupStatusResponse> getSetupStatus() {
        log.info("Checking system setup status");

        SetupStatusResponse response = setupService.getSetupStatus();

        log.debug("System setup status: {}", response.isSetupComplete());
        return ResponseEntity.ok(response);
    }
}
