package com.lynus.cs203.services;

import com.lynus.cs203.dtos.request.ChangePasswordRequest;
import com.lynus.cs203.dtos.request.CreateUserRequest;
import com.lynus.cs203.dtos.request.UpdateUserRequest;
import com.lynus.cs203.dtos.response.UserDto;
import com.lynus.cs203.entities.UserProfile;
import com.lynus.cs203.exceptions.EmailAlreadyExistsException;
import com.lynus.cs203.exceptions.InvalidPasswordException;
import com.lynus.cs203.exceptions.UserNotFoundException;
import com.lynus.cs203.mappers.UserMapper;
import com.lynus.cs203.entities.User;
import com.lynus.cs203.repositories.UserProfileRepository;
import com.lynus.cs203.repositories.UserRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final UserProfileRepository userProfileRepository;

    /* DTO Methods */
    public List<UserDto> getAllUsersAsDto(String sort) {
        List<User> users = getAllUsers(sort);
        return users.stream()
                .map(userMapper::toDto)
                .toList();
    }

    public UserDto getUserByEmailAsDto(String email) {
        User user = getUserByEmail(email);
        return userMapper.toDto(user);
    }

    public UserDto getUserByIdAsDto(String id) {
        User user = getUserById(id);
        return userMapper.toDto(user);
    }

    public UserDto createUserAsDto(CreateUserRequest request) {
        User user = createUser(request);
        return userMapper.toDto(user);
    }

    public UserDto updateUserAsDto(String id, UpdateUserRequest request) {
        User user = updateUser(id, request);
        return userMapper.toDto(user);
    }

    public void deleteUser(String id) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        userRepository.delete(user);
    }

    public void changePassword(String id, ChangePasswordRequest request) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new InvalidPasswordException("Old password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    /* Entity Methods */
    private List<User> getAllUsers(String sort) {
        String validSort = Set.of("email", "createdAt", "updatedAt").contains(sort) ? sort : "createdAt";

        // Use EntityGraph to fetch profiles eagerly
        return userRepository.findAll(Sort.by(validSort));
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found")
                );
    }

    private User getUserById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    private User createUser(CreateUserRequest request) {
        // Check if email is unique
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists: " + request.getEmail());
        }

        var user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        User savedUser = userRepository.save(user);

        // Create user profile
        UserProfile profile = new UserProfile();
        profile.setUserId(savedUser.getUserId());
        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        userProfileRepository.save(profile.getUser());

        // Reload user to include profile
        savedUser = userRepository.findById(savedUser.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found after creation"));

        return savedUser;
    }

    private User updateUser(String id, UpdateUserRequest request) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        userMapper.update(request, user);

        // Update profile if name fields are provided
        if (request.getFirstName() != null || request.getLastName() != null) {
            UserProfile profile = user.getUserProfile();
            if (profile == null) {
                profile = new UserProfile();
                profile.setUserId(user.getUserId());
                profile.setUser(user);
                user.setUserProfile(profile);
            }
            if (request.getFirstName() != null) {
                profile.setFirstName(request.getFirstName());
            }
            if (request.getLastName() != null) {
                profile.setLastName(request.getLastName());
            }
            userProfileRepository.save(profile.getUser());
        }

        User updatedUser = userRepository.save(user);

        // Reload user to include updated profile
        updatedUser = userRepository.findById(updatedUser.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found after update"));

        return updatedUser;
    }

}
