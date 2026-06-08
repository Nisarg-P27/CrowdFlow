package com.nisarg.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimitingService {

    private static final String LOGIN_ENDPOINT = "/api/auth/login";
    private static final String REGISTER_ENDPOINT = "/api/auth/register";
    private static final String REFRESH_ENDPOINT = "/api/auth/refresh";

    private final ProxyManager<String> proxyManager;

    public boolean tryConsume(String requestUri, String clientIp) {

        if (!isRateLimitedEndpoint(requestUri)) {
            return true;
        }

        String action = requestUri.substring(
                requestUri.lastIndexOf("/") + 1
        );

        String bucketKey = "rate-limit:" + action + ":" + clientIp;

//        Bucket bucket = buckets.computeIfAbsent(
//                bucketKey,
//                key -> createBucketForEndpoint(requestUri)
//        );
        Bucket bucket = proxyManager.builder()
                .build(bucketKey, ()-> createBucketConfigurationForEndPoints(requestUri));

        return bucket.tryConsume(1);
    }

    private boolean isRateLimitedEndpoint(String requestUri) {
        return LOGIN_ENDPOINT.equals(requestUri)
                || REGISTER_ENDPOINT.equals(requestUri)
                || REFRESH_ENDPOINT.equals(requestUri);
    }

    private BucketConfiguration createBucketConfigurationForEndPoints(String requestUri) {

        Bandwidth bandwidth = null;
        if (LOGIN_ENDPOINT.equals(requestUri)) {
            bandwidth = Bandwidth.builder()
                    .capacity(5)
                    .refillGreedy(5, Duration.ofMinutes(5))
                    .build();
        }
        if (REGISTER_ENDPOINT.equals(requestUri)) {
            bandwidth = Bandwidth.builder()
                    .capacity(3)
                    .refillGreedy(3, Duration.ofMinutes(1))
                    .build();
        }
        if (REFRESH_ENDPOINT.equals(requestUri)) {
            bandwidth = Bandwidth.builder()
                    .capacity(10)
                    .refillGreedy(10, Duration.ofMinutes(1))
                    .build();
        }

        return BucketConfiguration.builder()
                .addLimit(bandwidth)
                .build();
    }


}
