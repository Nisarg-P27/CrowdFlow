package com.nisarg.unittests;

import com.nisarg.dtos.requests.LoginRequest;
import com.nisarg.dtos.requests.RefreshTokenRequest;
import com.nisarg.dtos.requests.RegisterRequest;
import com.nisarg.dtos.responses.AuthResponse;
import com.nisarg.entities.RefreshTokenEntity;
import com.nisarg.entities.UserEntity;
import com.nisarg.enums.UserRole;
import com.nisarg.exceptions.BadRequestException;
import com.nisarg.exceptions.UnauthorizedException;
import com.nisarg.repositories.UserRepository;
import com.nisarg.security.JwtService;
import com.nisarg.services.AuthService;
import com.nisarg.services.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private UserEntity savedUser;
    private String refreshToken;

    @BeforeEach
    void setup() {

        registerRequest = RegisterRequest.builder()
                .name("Nisarg")
                .email("NiSarg@gmail.com")
                .password("mock-password")
                .build();

        loginRequest = LoginRequest.builder()
                .email("NiSarg@gmail.com")
                .password("mock-password")
                .build();

        savedUser = new UserEntity();
        savedUser.setId(UUID.randomUUID());
        savedUser.setName("Nisarg");
        savedUser.setEmail("nisarg@gmail.com");
        savedUser.setPassword("encoded-password");
        savedUser.setRole(UserRole.USER);

        refreshToken = "mock-refresh-token";
    }

    @Test
    @DisplayName("register -> should return tokens and user dto on success")
    void register_shouldReturnAuthResponse_whenRequestIsValid() {

        when(userRepository.existsByEmail("nisarg@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode("mock-password")).thenReturn("encoded-password");
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);

        when(jwtService.generateToken(
                savedUser.getId().toString(),
                savedUser.getEmail(),
                savedUser.getRole()
        )).thenReturn("mocked.jwt.token");

        when(refreshTokenService.createRefreshToken(savedUser))
                .thenReturn(refreshToken);

        AuthResponse authResponse = authService.register(registerRequest);

        assertSoftly(softly -> {
            softly.assertThat(authResponse).isNotNull();
            softly.assertThat(authResponse.accessToken()).isEqualTo("mocked.jwt.token");
            softly.assertThat(authResponse.refreshToken()).isEqualTo("mock-refresh-token");
            softly.assertThat(authResponse.user().email()).isEqualTo("nisarg@gmail.com");
            softly.assertThat(authResponse.user().name()).isEqualTo("Nisarg");
            softly.assertThat(authResponse.user().role()).isEqualTo(UserRole.USER);
        });
    }

    @Test
    @DisplayName("register -> should throw BadRequestException when email already exists")
    void register_shouldReturnBadRequestException_whenEmailAlreadyExists() {

        when(userRepository.existsByEmail("nisarg@gmail.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Email already exists");

        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(jwtService);
        verifyNoInteractions(refreshTokenService);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register -> should lowercase email before saving")
    void register_shouldReturnLowerCase_beforeSaving() {

        when(userRepository.existsByEmail("nisarg@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode("mock-password")).thenReturn("encoded-password");
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);
        when(refreshTokenService.createRefreshToken(savedUser)).thenReturn(refreshToken);

        when(jwtService.generateToken(
                savedUser.getId().toString(),
                savedUser.getEmail(),
                savedUser.getRole()
        )).thenReturn("mocked.jwt.token");

        authService.register(registerRequest);

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());

        assertThat(userCaptor.getValue().getEmail()).isEqualTo("nisarg@gmail.com");
    }

    @Test
    @DisplayName("register -> password is encoded before saving")
    void register_shouldEncodePassword_beforeSaving() {

        when(userRepository.existsByEmail("nisarg@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode("mock-password")).thenReturn("encoded-password");
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);
        when(refreshTokenService.createRefreshToken(savedUser)).thenReturn(refreshToken);

        when(jwtService.generateToken(
                savedUser.getId().toString(),
                savedUser.getEmail(),
                savedUser.getRole()
        )).thenReturn("mocked.jwt.token");

        authService.register(registerRequest);

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());

        assertThat(userCaptor.getValue().getPassword()).isEqualTo("encoded-password");
        assertThat(userCaptor.getValue().getPassword()).isNotEqualTo("mock-password");
    }

    @Test
    @DisplayName("register -> role is always USER")
    void register_shouldAlwaysAssignUserRole() {

        when(userRepository.existsByEmail("nisarg@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode("mock-password")).thenReturn("encoded-password");
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);
        when(refreshTokenService.createRefreshToken(savedUser)).thenReturn(refreshToken);

        when(jwtService.generateToken(
                savedUser.getId().toString(),
                savedUser.getEmail(),
                savedUser.getRole()
        )).thenReturn("mocked.jwt.token");

        authService.register(registerRequest);

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());

        assertThat(userCaptor.getValue().getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    @DisplayName("login -> should return tokens + user dto")
    void login_shouldReturnAuthResponse_whenRequestIsValid() {

        when(userRepository.findByEmail("nisarg@gmail.com"))
                .thenReturn(Optional.of(savedUser));

        when(passwordEncoder.matches("mock-password", "encoded-password"))
                .thenReturn(true);

        when(refreshTokenService.createRefreshToken(savedUser))
                .thenReturn(refreshToken);

        when(jwtService.generateToken(
                savedUser.getId().toString(),
                savedUser.getEmail(),
                savedUser.getRole()
        )).thenReturn("mocked.jwt.token");

        AuthResponse authResponse = authService.login(loginRequest);

        assertSoftly(softly -> {
            softly.assertThat(authResponse.accessToken()).isEqualTo("mocked.jwt.token");
            softly.assertThat(authResponse.refreshToken()).isEqualTo("mock-refresh-token");
            softly.assertThat(authResponse.user().email()).isEqualTo("nisarg@gmail.com");
        });
    }

    @Test
    @DisplayName("login -> should throw when email not found")
    void login_shouldThrowUnauthorizedException_whenEmailNotFound() {

        when(userRepository.findByEmail("nisarg@gmail.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid credentials");

        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(jwtService);
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    @DisplayName("login -> should throw when password invalid")
    void login_shouldThrowUnauthorizedException_whenPasswordNotMatches() {

        when(userRepository.findByEmail("nisarg@gmail.com"))
                .thenReturn(Optional.of(savedUser));

        when(passwordEncoder.matches("mock-password", "encoded-password"))
                .thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid credentials");

        verifyNoInteractions(jwtService);
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    @DisplayName("login -> should lowercase email lookup")
    void login_shouldLowerCaseEmail() {

        when(userRepository.findByEmail("nisarg@gmail.com"))
                .thenReturn(Optional.of(savedUser));

        when(passwordEncoder.matches("mock-password", "encoded-password"))
                .thenReturn(true);

        when(refreshTokenService.createRefreshToken(savedUser))
                .thenReturn(refreshToken);

        when(jwtService.generateToken(
                savedUser.getId().toString(),
                savedUser.getEmail(),
                savedUser.getRole()
        )).thenReturn("mocked.jwt.token");

        authService.login(loginRequest);

        verify(userRepository).findByEmail("nisarg@gmail.com");
        verify(userRepository, never()).findByEmail("NiSarg@gmail.com");
    }

    @Test
    @DisplayName("refresh -> should rotate token and return new auth response")
    void refresh_shouldReturnNewTokens() {

        RefreshTokenRequest request =
                new RefreshTokenRequest("old-refresh-token");

        when(refreshTokenService.rotateRefreshToken("old-refresh-token"))
                .thenReturn(refreshToken);

        when(jwtService.generateToken(
                savedUser.getId().toString(),
                savedUser.getEmail(),
                savedUser.getRole()
        )).thenReturn("new-access-token");

        AuthResponse response = authService.refreshAccessToken(request);

        assertSoftly(softly -> {
            softly.assertThat(response.accessToken()).isEqualTo("new-access-token");
            softly.assertThat(response.refreshToken()).isEqualTo("mock-refresh-token");
            softly.assertThat(response.user().email()).isEqualTo("nisarg@gmail.com");
        });
    }
}