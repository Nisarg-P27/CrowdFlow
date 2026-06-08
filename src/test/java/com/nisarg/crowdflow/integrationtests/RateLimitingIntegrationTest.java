package com.nisarg.crowdflow.integrationtests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nisarg.dtos.requests.LoginRequest;
import com.nisarg.dtos.requests.RefreshTokenRequest;
import com.nisarg.dtos.requests.RegisterRequest;
import com.nisarg.dtos.responses.AuthResponse;
import com.nisarg.security.RateLimitingFilter;
import com.nisarg.repositories.UserRepository;
import com.nisarg.security.RateLimitingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.rate-limiting.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RateLimitingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RateLimitingFilter rateLimitingFilter;

    @Autowired
    private RateLimitingService rateLimitingService;

    @BeforeEach
    void cleanup() {
        userRepository.deleteAll();
        rateLimitingService.clearBuckets();
    }

    @Test
    @DisplayName("login should return too many requests when rate limit exceeded")
    void login_shouldReturnTooManyRequests_whenRateLimitExceeded() throws Exception {

        RegisterRequest registerRequest = new RegisterRequest(
                "Nisarg",
                "nisarg@gmail.com",
                "password123"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        LoginRequest invalidLoginRequest = new LoginRequest(
                "nisarg@gmail.com",
                "wrongpassword"
        );

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidLoginRequest)))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidLoginRequest)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("register should return too many requests when rate limit exceeded")
    void register_shouldReturnTooManyRequests_whenRateLimitExceeded() throws Exception {

        for (int i = 0; i < 3; i++) {

            RegisterRequest registerRequest = new RegisterRequest(
                    "Nisarg",
                    "nisarg" + i + "@gmail.com",
                    "password123"
            );

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isOk());
        }

        RegisterRequest blockedRequest = new RegisterRequest(
                "Nisarg",
                "blocked@gmail.com",
                "password123"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blockedRequest)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("refresh should return too many requests when rate limit exceeded")
    void refresh_shouldReturnTooManyRequests_whenRateLimitExceeded() throws Exception {

        RegisterRequest registerRequest = new RegisterRequest(
                "Nisarg",
                "nisarg@gmail.com",
                "password123"
        );

        String registerResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        AuthResponse authResponse =
                objectMapper.readValue(registerResponse, AuthResponse.class);

        String currentRefreshToken = authResponse.refreshToken();

        for (int i = 0; i < 10; i++) {

            RefreshTokenRequest refreshRequest =
                    new RefreshTokenRequest(currentRefreshToken);

            String refreshResponse = mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshRequest)))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            AuthResponse refreshedResponse =
                    objectMapper.readValue(refreshResponse, AuthResponse.class);

            currentRefreshToken = refreshedResponse.refreshToken();
        }

        RefreshTokenRequest blockedRequest =
                new RefreshTokenRequest(currentRefreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blockedRequest)))
                .andExpect(status().isTooManyRequests());
    }
}