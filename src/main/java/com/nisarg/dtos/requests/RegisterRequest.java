package com.nisarg.dtos.requests;

import com.nisarg.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.springframework.context.annotation.Role;

@Builder
public record RegisterRequest(

        @NotBlank(message = "Name is required")
        @Size(min = 2,max = 60)
        String name,

        @Email(message = "Invalid email format")
        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password

//        @NotBlank(message = "Role is required")
//        UserRole userRole
) {
}