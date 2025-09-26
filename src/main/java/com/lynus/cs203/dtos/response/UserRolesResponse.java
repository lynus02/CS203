package com.lynus.cs203.dtos.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserRolesResponse {
    private String userId;
    private List<String> roles;
}
