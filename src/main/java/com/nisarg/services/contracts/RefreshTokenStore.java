package com.nisarg.services.contracts;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenStore {
    void store(UUID userId, String refreshToken, Duration ttl);

    Optional<UUID> findUserIdByToken(String refreshToken);

    Optional<String> findTokenByUserId(UUID userId);

    void deleteByUserId(UUID userId);

    void deleteByToken(String refreshToken);
}
