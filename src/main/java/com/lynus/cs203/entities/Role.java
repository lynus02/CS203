package com.lynus.cs203.entities;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Role {
    USER(1, "USER", "Regular user with limited access"),
    ADMIN(2, "ADMIN", "Administrator with full access");

    private final int id;
    private final String name;
    private final String description;

    public static Role fromId(int id) {
        for (Role role : Role.values()) {
            if (role.getId() == id) {
                return role;
            }
        }
        throw new IllegalArgumentException("No role found with id: " + id);
    }

    public static Role fromName(String name) {
        for (Role role : Role.values()) {
            if (role.getName().equalsIgnoreCase(name)) {
                return role;
            }
        }
        throw new IllegalArgumentException("No role found with name: " + name);
    }
}
