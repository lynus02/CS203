package com.lynus.cs203.mappers;

import com.lynus.cs203.dtos.request.CreateUserRequest;
import com.lynus.cs203.dtos.request.UpdateUserRequest;
import com.lynus.cs203.dtos.response.UserDto;
import com.lynus.cs203.entities.User;
import com.lynus.cs203.entities.UserRole;
import org.mapstruct.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "firstName", expression = "java(getFirstName(user))")
    @Mapping(target = "lastName", expression = "java(getLastName(user))")
    @Mapping(target = "avatarUrl", expression = "java(getAvatarUrl(user))")
    @Mapping(target = "roles", expression = "java(getUserRoles(user))")
    UserDto toDto(User user);

    default String getFirstName(User user) {
        return user.getUserProfile() != null ? user.getUserProfile().getFirstName() : null;
    }

    default String getLastName(User user) {
        return user.getUserProfile() != null ? user.getUserProfile().getLastName() : null;
    }

    default String getAvatarUrl(User user) {
        return user.getUserProfile() != null ? user.getUserProfile().getAvatarUrl() : null;
    }

    default List<String> getUserRoles(User user) {
        if (user.getUserRoles() == null || user.getUserRoles().isEmpty()) {
            return List.of();
        }
        return user.getUserRoles().stream()
                .map(UserRole::getRole)
                .map(role -> role.getName())
                .collect(Collectors.toList());
    }


    default LocalDateTime map(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneOffset.UTC) : null;
    }

    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "userProfile", ignore = true)
    @Mapping(target = "userRoles", ignore = true)
    User toEntity(CreateUserRequest createUserRequest);

    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "userProfile", ignore = true)
    @Mapping(target = "userRoles", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(UpdateUserRequest request, @MappingTarget User user);
}
