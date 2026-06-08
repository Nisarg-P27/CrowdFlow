package com.nisarg.dtos;

import com.nisarg.enums.UserRole;

import java.util.UUID;

public record UserDTO(
        UUID id,
        String email,
        String name,
        UserRole role
) {
}