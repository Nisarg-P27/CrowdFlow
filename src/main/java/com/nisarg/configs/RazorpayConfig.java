package com.nisarg.configs;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RazorpayConfig.RazorpayProperties.class)
public class RazorpayConfig {

    @Bean
    public RazorpayClient razorpayClient(
            RazorpayProperties properties
    ) throws RazorpayException {

        return new RazorpayClient(
                properties.keyId(),
                properties.keySecret()
        );
    }

    @ConfigurationProperties(prefix = "razorpay")
    public record RazorpayProperties(
            String keyId,
            String keySecret,
            String webhookSecret
    ) {
    }
}