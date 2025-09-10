package com.lynus.cs203.mappers;

import com.lynus.cs203.dtos.UserDto;
import com.lynus.cs203.entities.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDto(User user);
}
