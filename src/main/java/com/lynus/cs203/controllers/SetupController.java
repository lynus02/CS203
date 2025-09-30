package com.lynus.cs203.controllers;

import com.lynus.cs203.dtos.request.CreateUserRequest;
import com.lynus.cs203.dtos.response.AdminCreationResponse;
import com.lynus.cs203.dtos.response.ErrorResponse;
import com.lynus.cs203.dtos.response.SetupStatusResponse;
import com.lynus.cs203.services.SetupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/setup")
public class SetupController {
    private final SetupService setupService;

    @Operation(
            summary = "Create first admin user",
            description = "Initialize the system by creating the first admin user. This endpoint is only available when no admin users exist in the system."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "First admin user created successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AdminCreationResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request body or validation errors",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/admin")
    public ResponseEntity<AdminCreationResponse> createFirstAdmin(
            @Parameter(
                    description = "Admin user creation details",
                    required = true,
                    schema = @Schema(implementation = CreateUserRequest.class))
            @Valid @RequestBody CreateUserRequest request
    ) {
        log.info("POST /setup/admin - Creating first admin user");

        AdminCreationResponse response = setupService.createFirstAdmin(request);

        log.info("Successfully processed admin creation request");
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get system setup status",
            description = "Check if the system has been initialized (i.e., whether an admin user exists)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Setup status retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SetupStatusResponse.class)))
    })
    @GetMapping("/status")
    public ResponseEntity<SetupStatusResponse> getSetupStatus() {
        log.info("GET /setup/status - Checking setup status");

        SetupStatusResponse response = setupService.getSetupStatus();

        log.info("Successfully retrieved setup status");
        return ResponseEntity.ok(response);
    }
}
