package com.lynus.cs203.controllers;

import com.lynus.cs203.services.BlockchainAuditService;
import com.lynus.cs203.dtos.response.AuditResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit")
@AllArgsConstructor
public class AuditController {

    private final BlockchainAuditService blockchainAuditService;

    @PostMapping("/check")
    public ResponseEntity<AuditResponse> check() {
        AuditResponse result = blockchainAuditService.audit();
        if (result.isError()) {
            return ResponseEntity.status(500).body(result);
        }
        return ResponseEntity.ok(result);
    }
}
