package com.lynus.cs203.services;

import com.lynus.cs203.dtos.request.ChangePasswordRequest;
import com.lynus.cs203.dtos.request.CreateUserRequest;
import com.lynus.cs203.dtos.request.UpdateUserRequest;
import com.lynus.cs203.dtos.response.CreateUserResponse;
import com.lynus.cs203.dtos.response.PasswordChangeResponse;
import com.lynus.cs203.dtos.response.UserDto;
import com.lynus.cs203.entities.*;
import com.lynus.cs203.exceptions.EmailAlreadyExistsException;
import com.lynus.cs203.exceptions.InvalidPasswordException;
import com.lynus.cs203.exceptions.UserNotFoundException;
import com.lynus.cs203.mappers.UserMapper;
import com.lynus.cs203.repositories.UserProfileRepository;
import com.lynus.cs203.repositories.UserRepository;
import com.lynus.cs203.repositories.UserRoleRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final UserProfileRepository userProfileRepository;
    private final UserRoleRepository userRoleRepository;
    private final JwtService jwtService;

    /* DTO Methods */
    public List<UserDto> getAllUsersAsDto(String sort) {
        log.debug("Retrieving all users as DTOs with sort: {}", sort);
        List<User> users = getAllUsers(sort);
        log.debug("Converted {} users to DTOs", users.size());
        return users.stream()
                .map(userMapper::toDto)
                .toList();
    }

    public UserDto getUserByEmailAsDto(String email) {
        log.debug("Retrieving user as DTO by email: {}", email);
        User user = getUserByEmail(email);
        return userMapper.toDto(user);
    }

    public UserDto getUserByIdAsDto(String id) {
        log.debug("Retrieving user as DTO by ID: {}", id);
        User user = getUserById(id);
        return userMapper.toDto(user);
    }

    public UserDto createUserAsDto(CreateUserRequest request) {
        log.debug("Creating user as DTO for email: {}", request.getEmail());
        User user = createUser(request);
        return userMapper.toDto(user);
    }

    public UserDto updateUserAsDto(String id, UpdateUserRequest request) {
        log.debug("Updating user as DTO for ID: {}", id);
        User user = updateUser(id, request);
        return userMapper.toDto(user);
    }

    public void deleteUser(String id) {
        log.info("Deleting user with ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found for deletion: {}", id);
                    return new UserNotFoundException("User not found with id: " + id);
                });

        userRepository.delete(user);
        log.info("Successfully deleted user: {}", id);
    }

    public PasswordChangeResponse changePassword(String id, ChangePasswordRequest request) {
        log.info("Changing password for user: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found for password change: {}", id);
                    return new UserNotFoundException("User not found with id: " + id);
                });

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            log.warn("Invalid old password provided for user: {}", id);
            throw new InvalidPasswordException("Old password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Successfully changed password for user: {}", id);
        return PasswordChangeResponse.builder()
                .message("Password changed successfully")
                .userId(user.getUserId())
                .build();
    }

    public void resetPasswordByEmail(String email, String newPassword) {
        log.info("Resetting password for user with email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found for password reset: {}", email);
                    return new UserNotFoundException("User not found with email: " + email);
                });

        log.debug("Encoding new password for user: {}", user.getUserId());
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(java.time.Instant.now());

        userRepository.save(user);

        log.info("Successfully reset password for user: {} (ID: {})", email, user.getUserId());
    }

    /* Entity Methods */
    public List<User> getAllUsers(String sort) {
        log.debug("Retrieving all users with sort parameter: {}", sort);
        String validSort = Set.of("email", "createdAt", "updatedAt").contains(sort) ? sort : "createdAt";
        log.trace("Using validated sort parameter: {}", validSort);

        List<User> users = userRepository.findAll(Sort.by(validSort));
        log.debug("Retrieved {} users from database", users.size());
        return users;
    }

    public User getUserByEmail(String email) {
        log.debug("Retrieving user by email: {}", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found by email: {}", email);
                    return new UserNotFoundException("User not found with email: " + email);
                });
    }

    public User getUserById(String id) {
        log.debug("Retrieving user by ID: {}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found by ID: {}", id);
                    return new UserNotFoundException("User not found with id: " + id);
                });
    }

    @Transactional
    public User createUser(CreateUserRequest request) {
        log.info("Creating new user with email: {}", request.getEmail());

        // Check if email is unique
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Email already exists: {}", request.getEmail());
            throw new EmailAlreadyExistsException("Email already exists: " + request.getEmail());
        }

        log.debug("Mapping create request to user entity");
        var user = userMapper.toEntity(request);

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setIsActive(true);
        user.setCreatedAt(java.time.Instant.now());
        user.setUpdatedAt(java.time.Instant.now());

        // Save user first to get the generated ID
        log.debug("Saving user entity to database");
        User savedUser = userRepository.save(user);
        log.debug("Generated user ID: {}", savedUser.getUserId());

        // Now create user profile with the saved user
        log.debug("Creating user profile for user: {}", savedUser.getUserId());
        UserProfile profile = new UserProfile();
        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setUser(savedUser);

        // Save the profile
        UserProfile savedProfile = userProfileRepository.save(profile);
        log.debug("Saved user profile for user: {}", savedUser.getUserId());

        // Set the profile back to the user to ensure the relationship is complete
        savedUser.setUserProfile(savedProfile);

        // Assign default USER role
        log.debug("Assigning default USER role to user: {}", savedUser.getUserId());
        assignRole(savedUser.getUserId(), Role.USER);

        log.info("Successfully created user - ID: {}, Email: {}", savedUser.getUserId(), request.getEmail());
        return savedUser;
    }

    public CreateUserResponse createUserAndReturnToken(CreateUserRequest request) {
        User savedUser = createUser(request);

        // Generate token AFTER saving the user
        String token = jwtService.generateAccessToken(savedUser);

        return new CreateUserResponse(
                savedUser.getUserId(),
                token,
                savedUser.getEmail(),
                savedUser.getUserProfile().getFirstName(),
                savedUser.getUserProfile().getLastName(),
                userRoleRepository.findByUserUserId(savedUser.getUserId())
                        .stream()
                        .map(ur -> ur.getRole().getName())
                        .toList()
        );
    }

    @Transactional
    public User updateUser(String id, UpdateUserRequest request) {
        log.info("Updating user with ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found for update: {}", id);
                    return new UserNotFoundException("User not found with id: " + id);
                });

        // Check email uniqueness if email is being updated
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                log.warn("Email already exists for update: {}", request.getEmail());
                throw new EmailAlreadyExistsException("Email already exists: " + request.getEmail());
            }
        }

        log.debug("Mapping update request to user entity");
        userMapper.update(request, user);

        // Update profile if name fields are provided
        if (request.getFirstName() != null || request.getLastName() != null) {
            log.debug("Updating user profile for user: {}", id);
            UserProfile profile = user.getUserProfile();
            if (request.getFirstName() != null) {
                profile.setFirstName(request.getFirstName());
            }
            if (request.getLastName() != null) {
                profile.setLastName(request.getLastName());
            }
            userProfileRepository.save(profile);
            log.debug("Saved updated profile for user: {}", id);
        }

        log.debug("Saving updated user entity to database");
        User updatedUser = userRepository.save(user);

        // Reload user to include updated profile
        log.debug("Reloading user with updated profile information");
        updatedUser = userRepository.findById(updatedUser.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found after update"));

        log.info("Successfully updated user: {}", id);
        return updatedUser;
    }

    public void assignRole(String userId, Role role) {
        log.info("Assigning role {} to user: {}", role.getName(), userId);

        User user = getUserById(userId);

        // Check if user already has this role
        if (hasRole(userId, role)) {
            log.debug("User {} already has role {}", userId, role.getName());
            return;
        }

        UserRole userRole = new UserRole();
        UserRoleId userRoleId = new UserRoleId();
        userRoleId.setUserId(userId);
        userRoleId.setRoleId((short) role.getId());

        userRole.setId(userRoleId);
        userRole.setUser(user);
        userRole.setRole(role);
        userRole.setAssignedAt(LocalDateTime.now());

        userRoleRepository.save(userRole);
        log.info("Successfully assigned role {} to user: {}", role.getName(), userId);
    }

    public boolean hasRole(String userId, Role role) {
        log.trace("Checking if user {} has role: {}", userId, role.getName());
        return userRoleRepository.existsByUserUserIdAndRole(userId, role);
    }

    public void removeRole(String userId, Role role) {
        log.info("Removing role {} from user: {}", role.getName(), userId);

        // Verify if user exists
        getUserById(userId);    // will throw exception if user not found

        userRoleRepository.deleteByUserUserIdAndRole(userId, role);
        log.info("Successfully removed role {} from user: {}", role.getName(), userId);
    }

    public List<String> getUserRoles(String userId) {
        log.debug("Retrieving roles for user: {}", userId);

        // Verify user exists
        getUserById(userId);

        List<String> roles = userRoleRepository.findByUserUserId(userId)
                .stream()
                .map(userRole -> userRole.getRole().getName())
                .toList();

        log.debug("Found {} roles for user: {}", roles.size(), userId);
        return roles;
    }

    public boolean adminExists() {
        log.trace("Checking if admin user exists");
        boolean exists = userRoleRepository.existsByRole(Role.ADMIN);
        log.trace("Admin user exists: {}", exists);
        return exists;
    }

}
