package com.nisarg.infrastructure;


import com.nisarg.services.contracts.RefreshTokenStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String USER_KEY_PREFIX = "refresh:user:";
    private static final String TOKEN_KEY_PREFIX = "refresh:token:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void store(UUID userId, String refreshToken, Duration ttl) {
        redisTemplate.opsForValue()
                .set(buildUserKey(userId), refreshToken, ttl);

        redisTemplate.opsForValue()
                .set(buildTokenKey(refreshToken), userId.toString(), ttl);
    }

    @Override
    public Optional<UUID> findUserIdByToken(String refreshToken) {
        String userId = redisTemplate.opsForValue().get(buildTokenKey(refreshToken));

        if(userId == null){
            return Optional.empty();
        }
        return Optional.of(UUID.fromString(userId));
    }

    @Override
    public Optional<String> findTokenByUserId(UUID userId) {

        String refreshToken = redisTemplate.opsForValue().get(buildUserKey(userId));

        if(refreshToken == null){
            return Optional.empty();
        }
        return Optional.of(refreshToken);
    }

    @Override
    public void deleteByUserId(UUID userId) {
        String refreshToken = redisTemplate.opsForValue().get(buildUserKey(userId));

        if(refreshToken!=null){
            redisTemplate.delete(buildTokenKey(refreshToken));
        }
        redisTemplate.delete(buildUserKey(userId));
    }

    @Override
    public void deleteByToken(String refreshToken) {
        String userId = redisTemplate.opsForValue().get(buildTokenKey(refreshToken));

        if(userId!=null){
            redisTemplate.delete(buildUserKey(UUID.fromString(userId)));
        }

        redisTemplate.delete(buildTokenKey(refreshToken));
    }

    private String buildUserKey(UUID userId) {
        return USER_KEY_PREFIX + userId;
    }
    private String buildTokenKey(String refreshToken) { return TOKEN_KEY_PREFIX + refreshToken; };
}