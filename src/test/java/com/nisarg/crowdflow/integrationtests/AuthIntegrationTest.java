package com.nisarg.integrationtests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nisarg.dtos.requests.LoginRequest;
import com.nisarg.dtos.requests.RegisterRequest;
import com.nisarg.entities.UserEntity;
import com.nisarg.enums.UserRole;
import com.nisarg.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("tickethub_test")
                    .withUsername("postgres")
                    .withPassword("password");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
    void cleanup() {
        userRepository.deleteAll();
    }

    // REGISTER TESTS

    @Test
    void shouldRegisterUserSuccessfully() throws Exception {

        RegisterRequest registerRequest = new RegisterRequest(
                "Nisarg",
                "nisarg@gmail.com",
                "password123"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());

        UserEntity savedUser =
                userRepository.findByEmail("nisarg@gmail.com").orElseThrow();

        assertThat(savedUser.getName()).isEqualTo("Nisarg");
        assertThat(savedUser.getEmail()).isEqualTo("nisarg@gmail.com");
        assertThat(passwordEncoder.matches(
                "password123",
                savedUser.getPassword()
        )).isTrue();
    }

    @Test
    void shouldRejectDuplicateEmail() throws Exception {

        UserEntity existingUser = new UserEntity();
        existingUser.setName("Nisarg");
        existingUser.setEmail("nisarg@gmail.com");
        existingUser.setPassword("password123");
        existingUser.setRole(UserRole.USER);

        userRepository.save(existingUser);

        RegisterRequest request = new RegisterRequest(
                "Nisarg",
                "nisarg@gmail.com",
                "password123"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectRegistrationWithInvalidEmail() throws Exception {

        RegisterRequest request = new RegisterRequest(
                "Nisarg",
                "not-an-email",
                "password123"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assertThat(userRepository.findByEmail("not-an-email")).isEmpty();
    }

    @Test
    void shouldRejectRegistrationWithBlankPassword() throws Exception {

        RegisterRequest request = new RegisterRequest(
                "Nisarg",
                "nisarg@gmail.com",
                ""
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assertThat(userRepository.findByEmail("nisarg@gmail.com")).isEmpty();
    }

    // LOGIN TESTS

    @Test
    void shouldLoginSuccessfully() throws Exception {

        UserEntity user = new UserEntity();
        user.setName("Nisarg");
        user.setEmail("nisarg@gmail.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole(UserRole.USER);
        userRepository.save(user);

        LoginRequest request = new LoginRequest(
                "nisarg@gmail.com",
                "password123"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.user.email").value("nisarg@gmail.com"));
    }

    @Test
    void shouldRejectLoginWithWrongPassword() throws Exception {

        UserEntity user = new UserEntity();
        user.setName("Nisarg");
        user.setEmail("nisarg@gmail.com");
        user.setPassword(passwordEncoder.encode("correctPassword"));
        user.setRole(UserRole.USER);
        userRepository.save(user);

        LoginRequest request = new LoginRequest(
                "nisarg@gmail.com",
                "wrongPassword"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectLoginWhenUserDoesNotExist() throws Exception {

        LoginRequest request = new LoginRequest(
                "ghost@gmail.com",
                "password123"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectLoginWithInvalidEmail() throws Exception {

        LoginRequest request = new LoginRequest(
                "bad-email",
                "password123"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // REFRESH TESTS

    @Test
    void shouldRefreshAccessTokenSuccessfully() throws Exception {

        UserEntity user = new UserEntity();
        user.setName("Nisarg");
        user.setEmail("nisarg@gmail.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole(UserRole.USER);
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest(
                "nisarg@gmail.com",
                "password123"
        );

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode loginJson = objectMapper.readTree(loginResponse);
        String refreshToken = loginJson.get("refreshToken").asText();

        String refreshRequestBody = """
                {
                  "refreshToken": "%s"
                }
                """.formatted(refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshRequestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    void shouldRejectInvalidRefreshToken() throws Exception {

        String refreshRequestBody = """
                {
                  "refreshToken": "fake-invalid-token"
                }
                """;

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshRequestBody))
                .andExpect(status().isBadRequest());
    }

    // LOGOUT TESTS

    @Test
    void shouldInvalidateRefreshTokenAfterLogout() throws Exception {

        UserEntity user = new UserEntity();
        user.setName("Nisarg");
        user.setEmail("nisarg@gmail.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole(UserRole.USER);
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest(
                "nisarg@gmail.com",
                "password123"
        );

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode loginJson = objectMapper.readTree(loginResponse);

        String accessToken = loginJson.get("accessToken").asText();
        String refreshToken = loginJson.get("refreshToken").asText();

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        String refreshRequestBody = """
                {
                  "refreshToken": "%s"
                }
                """.formatted(refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshRequestBody))
                .andExpect(status().isBadRequest());
    }
}