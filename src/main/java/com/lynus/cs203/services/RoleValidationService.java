package com.lynus.cs203.services;

import com.lynus.cs203.entities.Role;
import com.lynus.cs203.exceptions.InvalidRoleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RoleValidationService {

    public Role validateAndGetRole(String roleName) {
        try {
            return Role.fromName(roleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid role name provided: {}", roleName);
            throw new InvalidRoleException("Invalid role name: " + roleName);
        }
    }
}
