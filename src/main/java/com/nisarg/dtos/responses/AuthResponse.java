package com.nisarg.dtos.responses;

import com.nisarg.dtos.UserDTO;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserDTO user
) {
}