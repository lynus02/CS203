package com.lynus.cs203.dtos.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoleOperationResponse {
    private String message;
    private String userId;
    private String role;
}
