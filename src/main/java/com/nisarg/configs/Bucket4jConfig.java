package com.nisarg.configs;


import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Bucket4jConfig {

    @Bean
    public StatefulRedisConnection<String, byte[]> redisConnection(
            @Value("${spring.data.redis.host}") String host,
            @Value("${spring.data.redis.port}") int port
    ) {

        RedisURI redisUri = RedisURI.builder()
                .withHost(host)
                .withPort(port)
                .build();

        RedisClient redisClient = RedisClient.create(redisUri);

        RedisCodec<String, byte[]> redisCodec = RedisCodec.of(
                StringCodec.UTF8,
                ByteArrayCodec.INSTANCE
        );
        return redisClient.connect(redisCodec);
    }

    @Bean
    public ProxyManager<String> proxyManager( StatefulRedisConnection<String, byte[]> redisConnection ) {

        return Bucket4jLettuce.casBasedBuilder(redisConnection)
                .build();
    }
}