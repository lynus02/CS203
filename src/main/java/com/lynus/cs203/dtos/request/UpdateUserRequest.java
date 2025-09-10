package com.lynus.cs203.dtos.request;

import lombok.Data;

@Data
public class UpdateUserRequest {
    public String name;
    public String email;
}
