package com.lynus.cs203.services;

import com.lynus.cs203.exceptions.UserNotFoundException;
import com.lynus.cs203.repositories.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    private final UserService userService;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.info("Loading user details for email: {}", email);

        var user = userRepository.findByEmailWithProfile(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        log.debug("Found user with ID: {} for email: {}", user.getUserId(), email);

        UserDetails userDetails = new User(
                user.getUserId(),
//                user.getEmail(),
                user.getPassword(),
                getAuthorities(user)
        );

        log.info("Successfully loaded user details for: {}", email);
        return userDetails;
    }

    private List<GrantedAuthority> getAuthorities(com.lynus.cs203.entities.User user) {
        log.debug("Getting authorities for user: {}", user.getUserId());

        List<String> roles = userService.getUserRoles(user.getUserId());
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
    }
}
