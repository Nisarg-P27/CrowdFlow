package com.nisarg.services;


import com.nisarg.dtos.UserDTO;
import com.nisarg.dtos.requests.LoginRequest;
import com.nisarg.dtos.requests.RefreshTokenRequest;
import com.nisarg.dtos.requests.RegisterRequest;
import com.nisarg.dtos.responses.AuthResponse;
import com.nisarg.entities.UserEntity;
import com.nisarg.enums.UserRole;
import com.nisarg.exceptions.BadRequestException;
import com.nisarg.exceptions.ResourceNotFoundException;
import com.nisarg.exceptions.UnauthorizedException;
import com.nisarg.repositories.UserRepository;
import com.nisarg.security.JwtService;
import com.nisarg.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;


    public AuthResponse register(RegisterRequest request) {

        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            log.warn("Registration attempt with existing email. email={}",
                    request.email());
            throw new BadRequestException("Email already exists");
        }

        String encodedPassword =
                passwordEncoder.encode(request.password());

        UserEntity user = new UserEntity();

        user.setName(request.name());
        user.setEmail(email);
        user.setPassword(encodedPassword);

        if (email.startsWith("org")) {
            user.setRole(UserRole.ORGANIZER);
        } else {
            user.setRole(UserRole.USER);
        }

        UserEntity savedUser = userRepository.save(user);

        log.info("User registered successfully. userId={}, email={}, role={}",
                user.getId(),
                user.getEmail(),
                user.getRole());

        String refreshToken =
                refreshTokenService.createRefreshToken(savedUser);

        String accessToken = jwtService.generateToken(
                savedUser.getId().toString(),
                savedUser.getEmail(),
                savedUser.getRole()
        );

        return new AuthResponse(
                accessToken,
                refreshToken,
                mapToDto(savedUser)
        );
    }

    public AuthResponse login(LoginRequest request) {

        String email = request.email().toLowerCase();

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UnauthorizedException("Invalid credentials"));

        boolean passwordMatches = passwordEncoder.matches(
                request.password(),
                user.getPassword()
        );

        if (!passwordMatches) {
            log.warn("Failed login attempt. email={}",
                    request.email());
            throw new UnauthorizedException("Invalid credentials");
        }

        refreshTokenService.deleteActiveTokens(user.getId());

        String refreshToken =
                refreshTokenService.createRefreshToken(user);

        String accessToken = jwtService.generateToken(
                user.getId().toString(),
                user.getEmail(),
                user.getRole()
        );

        log.info("User login successful. userId={}, email={}",
                user.getId(),
                user.getEmail());

        return new AuthResponse(
                accessToken,
                refreshToken,
                mapToDto(user)
        );
    }

    public AuthResponse refreshAccessToken(RefreshTokenRequest request) {

        String refreshToken =
                refreshTokenService.rotateRefreshToken(request.refreshToken());

        UUID userId = refreshTokenService.findUserIdByToken(refreshToken);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String accessToken = jwtService.generateToken(
                userId.toString(),
                user.getEmail(),
                user.getRole()
        );

        log.info("Access token refreshed. userId={}", user.getId());

        return new AuthResponse(
                accessToken,
                refreshToken,
                mapToDto(user)
        );
    }

    public void logout() {

        UUID userId = SecurityUtils.getCurrentUserId();

        refreshTokenService.deleteActiveTokens(userId);

        log.info("User logged out. userId={}", userId);
    }

    private UserDTO mapToDto(UserEntity user) {

        return new UserDTO(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole()
        );
    }
}