package com.lynus.cs203.services;

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
                .orElseThrow(() -> {
                    log.warn("User not found for email: {}", email);
                    return new UsernameNotFoundException("User not found with email: " + email);
                });

        log.debug("Found user: {} (ID: {})", email, user.getUserId());

        if (!user.getIsActive()) {
            log.warn("User account is disabled: {}", email);
        }

        List<GrantedAuthority> authorities = getAuthorities(user);
        log.debug("User {} has {} authorities: {}", email, authorities.size(),
                authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()));

        UserDetails userDetails = User.builder()
                .username(user.getUserId())
                .password(user.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!user.getIsActive())
                .build();

        log.info("Successfully loaded user details for: {} with {} roles", email, authorities.size());
        return userDetails;
    }

    private List<GrantedAuthority> getAuthorities(com.lynus.cs203.entities.User user) {
        log.debug("Getting authorities for user ID: {}", user.getUserId());

        List<String> roles = userService.getUserRoles(user.getUserId());

        if (roles.isEmpty()) {
            log.warn("User {} has no roles assigned", user.getUserId());
        }

        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
    }
}
