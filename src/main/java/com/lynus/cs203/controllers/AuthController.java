package com.lynus.cs203.controllers;

import com.lynus.cs203.dtos.request.LoginRequest;
import com.lynus.cs203.dtos.response.JwtResponse;
import com.lynus.cs203.dtos.response.UserDto;
import com.lynus.cs203.services.JwtService;
import com.lynus.cs203.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        var token = jwtService.generateToken(request.getEmail());

        return ResponseEntity.ok(new JwtResponse(token));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String token = authHeader.substring(7);
        String userId = jwtService.extractUserId(token);
        UserDto user = userService.getUserByIdAsDto(userId);

        return ResponseEntity.ok(user);
    }

    // temporary for testing
    @PostMapping("/validate")
    public boolean validate(
            @RequestHeader("Authorization") String token
    ) {
        return jwtService.validateToken(token);
    }

}
