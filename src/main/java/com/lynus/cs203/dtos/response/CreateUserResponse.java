package com.lynus.cs203.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateUserResponse {
    private String userId;
    private String token;
    private String email;
    private String firstName;
    private String lastName;
    private List<String> roles;
}