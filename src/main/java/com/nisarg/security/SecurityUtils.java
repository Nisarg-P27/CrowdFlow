package com.nisarg.security;

import com.nisarg.entities.UserEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class  SecurityUtils {

    private SecurityUtils() {
    }


    public static UUID getCurrentUserId() {


        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof String userId) {
            return UUID.fromString(userId);
        }

        throw new IllegalStateException("Unexpected principal type: " + principal.getClass());
    }
}
