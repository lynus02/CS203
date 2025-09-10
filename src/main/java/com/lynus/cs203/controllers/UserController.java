package com.lynus.cs203.controllers;

import com.lynus.cs203.mappers.UserMapper;
import com.lynus.cs203.repositories.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @GetMapping
    public String getAllUsers() {
        return "List of users";
    }

    @GetMapping("/{id}")
    public String getUser() {
        return "User details by ID";
    }

    @PostMapping
    public String registerUser() {
        return "Create a new user";
    }

    @PutMapping("/{id}")
    public String updateUser() {
        return "Update user details";
    }

    @DeleteMapping("/{id}")
    public String deleteUser() {
        return "Delete a user";
    }

    @PostMapping("/{id}/change-password")
    public String changePassword() {
        return "Change user password";
    }

}
