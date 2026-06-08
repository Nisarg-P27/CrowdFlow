package com.nisarg.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisSmokeService {

    private final StringRedisTemplate redisTemplate;

    public void test() {
        redisTemplate.opsForValue().set(
                "temp",
                "abc",
                Duration.ofSeconds(30)
        );

        String value = redisTemplate.opsForValue().get("temp");

        System.out.println(value);
    }
}