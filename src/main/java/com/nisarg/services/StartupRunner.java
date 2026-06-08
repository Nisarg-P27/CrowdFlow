package com.nisarg.services;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StartupRunner implements CommandLineRunner {

    private final RedisSmokeService redisSmokeService;

    @Override
    public void run(String... args) {
        redisSmokeService.test();
    }
}