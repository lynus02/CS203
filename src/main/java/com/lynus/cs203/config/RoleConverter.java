package com.lynus.cs203.config;

import com.lynus.cs203.entities.Role;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Converter(autoApply = true)
public class RoleConverter implements AttributeConverter<Role, Short> {

    @Override
    public Short convertToDatabaseColumn(Role role) {
        Short result = role != null ? (short) role.getId() : null;
        log.debug("Converting Role to database column: {} -> {}", role, result);
        return result;
    }

    @Override
    public Role convertToEntityAttribute(Short id) {
        Role result = id != null ? Role.fromId(id.intValue()) : null;
        log.debug("Converting database column to Role: {} -> {}", id, result);
        return result;
    }

}
