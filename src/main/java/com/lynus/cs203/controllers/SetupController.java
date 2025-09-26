package com.lynus.cs203.controllers;

import com.lynus.cs203.dtos.request.CreateUserRequest;
import com.lynus.cs203.dtos.response.AdminCreationResponse;
import com.lynus.cs203.dtos.response.SetupStatusResponse;
import com.lynus.cs203.services.SetupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/setup")
public class SetupController {
    private final SetupService setupService;

    @PostMapping("/admin")
    public ResponseEntity<AdminCreationResponse> createFirstAdmin(
            @Valid @RequestBody CreateUserRequest request
    ) {
        log.info("POST /setup/admin - Creating first admin user");

        AdminCreationResponse response = setupService.createFirstAdmin(request);

        log.info("Successfully processed admin creation request");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<SetupStatusResponse> getSetupStatus() {
        log.info("GET /setup/status - Checking setup status");

        SetupStatusResponse response = setupService.getSetupStatus();

        log.info("Successfully retrieved setup status");
        return ResponseEntity.ok(response);
    }
}
