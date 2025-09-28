package com.lynus.cs203.config;

import com.lynus.cs203.entities.Role;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RoleConverter implements AttributeConverter<Role, Short> {

    @Override
    public Short convertToDatabaseColumn(Role role) {
        return role != null ? (short) role.getId() : null;
    }

    @Override
    public Role convertToEntityAttribute(Short id) {
        return id != null ? Role.fromId(id.intValue()) : null;
    }

}
