package com.lynus.cs203.mappers;

import com.lynus.cs203.dtos.request.CreateUserRequest;
import com.lynus.cs203.dtos.request.UpdateUserRequest;
import com.lynus.cs203.dtos.response.UserDto;
import com.lynus.cs203.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "firstName", expression = "java(getFirstName(user))")
    @Mapping(target = "lastName", expression = "java(getLastName(user))")
    @Mapping(target = "avatarUrl", expression = "java(getAvatarUrl(user))")
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
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "userProfile", ignore = true)
    @Mapping(target = "userRoles", ignore = true)
    void update(UpdateUserRequest request, @MappingTarget User user);
}
