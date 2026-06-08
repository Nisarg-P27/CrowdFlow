package com.nisarg.services;

import com.nisarg.entities.UserEntity;
import com.nisarg.exceptions.BadRequestException;
import com.nisarg.services.contracts.RefreshTokenStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenStore refreshTokenStore;

    @Value("${app.jwt.refresh-expiration}")
    private long refreshTokenExpirationMillis;

    public String createRefreshToken(UserEntity user) {

        String refreshToken = UUID.randomUUID().toString();

        refreshTokenStore.store(
                user.getId(),
                refreshToken,
                Duration.ofMillis(refreshTokenExpirationMillis)
        );

        log.info("Refresh token issued. userId={}", user.getId());

        return refreshToken;
    }

    public UUID validateRefreshToken(String refreshToken) {

        return refreshTokenStore.findUserIdByToken(refreshToken)
                .orElseThrow(() -> {
                    log.warn("Invalid or expired refresh token used.");
                    return new BadRequestException("Invalid refresh token");
                });
    }

    public void deleteActiveTokens(UUID userId) {
        refreshTokenStore.deleteByUserId(userId);

        log.info("Active refresh tokens deleted. userId={}", userId);
    }

    public String rotateRefreshToken(String token) {

        UUID userId = validateRefreshToken(token);

        refreshTokenStore.deleteByToken(token);

        String newRefreshToken = UUID.randomUUID().toString();

        refreshTokenStore.store(
                userId,
                newRefreshToken,
                Duration.ofMillis(refreshTokenExpirationMillis)
        );

        log.info("Refresh token rotated. userId={}", userId);

        return newRefreshToken;
    }

    public UUID findUserIdByToken(String refreshToken){
        return refreshTokenStore.findUserIdByToken(refreshToken)
                .orElseThrow(()->{
                    log.warn("Invalid or expired refresh token used.");
                    return new BadRequestException("Invalid refresh token");
                });

    }
}