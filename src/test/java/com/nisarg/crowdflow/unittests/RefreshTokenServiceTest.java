package com.nisarg.crowdflow.unittests;

import com.nisarg.entities.UserEntity;
import com.nisarg.exceptions.BadRequestException;
import com.nisarg.services.RefreshTokenService;
import com.nisarg.services.contracts.RefreshTokenStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenStore refreshTokenStore;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private UserEntity user;
    private UUID userId;

    @BeforeEach
    void setup() {

        ReflectionTestUtils.setField(
                refreshTokenService,
                "refreshTokenExpirationMillis",
                604800000L
        );

        userId = UUID.randomUUID();

        user = new UserEntity();
        user.setId(userId);
        user.setName("Nisarg");
        user.setEmail("nisarg@gmail.com");
    }

    @Test
    @DisplayName("createRefreshToken -> should create and store refresh token")
    void createRefreshToken_shouldCreateAndStoreRefreshToken() {

        String result = refreshTokenService.createRefreshToken(user);

        assertSoftly(softly -> {
            softly.assertThat(result).isNotNull();
            softly.assertThat(result).isNotBlank();
        });

        verify(refreshTokenStore).store(
                eq(userId),
                eq(result),
                eq(Duration.ofMillis(604800000L))
        );
    }

    @Test
    @DisplayName("validateRefreshToken -> should return userId when token valid")
    void validateRefreshToken_shouldReturnUserId_whenTokenValid() {

        when(refreshTokenStore.findUserIdByToken("valid-refresh-token"))
                .thenReturn(Optional.of(userId));

        UUID result =
                refreshTokenService.validateRefreshToken("valid-refresh-token");

        assertThat(result).isEqualTo(userId);
    }

    @Test
    @DisplayName("validateRefreshToken -> should throw when token invalid")
    void validateRefreshToken_shouldThrowBadRequestException_whenTokenInvalid() {

        when(refreshTokenStore.findUserIdByToken("invalid-token"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                refreshTokenService.validateRefreshToken("invalid-token"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid refresh token");
    }

    @Test
    @DisplayName("deleteActiveTokens -> should delete active tokens for user")
    void deleteActiveTokens_shouldDeleteActiveTokensForUser() {

        refreshTokenService.deleteActiveTokens(userId);

        verify(refreshTokenStore).deleteByUserId(userId);
    }

    @Test
    @DisplayName("rotateRefreshToken -> should rotate refresh token")
    void rotateRefreshToken_shouldRotateRefreshToken() {

        when(refreshTokenStore.findUserIdByToken("valid-refresh-token"))
                .thenReturn(Optional.of(userId));

        String result =
                refreshTokenService.rotateRefreshToken("valid-refresh-token");

        assertSoftly(softly -> {
            softly.assertThat(result).isNotNull();
            softly.assertThat(result).isNotBlank();
            softly.assertThat(result).isNotEqualTo("valid-refresh-token");
        });

        verify(refreshTokenStore).deleteByToken("valid-refresh-token");

        verify(refreshTokenStore).store(
                eq(userId),
                eq(result),
                eq(Duration.ofMillis(604800000L))
        );
    }

    @Test
    @DisplayName("rotateRefreshToken -> should throw when token invalid")
    void rotateRefreshToken_shouldThrowBadRequestException_whenTokenInvalid() {

        when(refreshTokenStore.findUserIdByToken("invalid-token"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                refreshTokenService.rotateRefreshToken("invalid-token"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid refresh token");

        verify(refreshTokenStore, never()).deleteByToken(any());
        verify(refreshTokenStore, never()).store(any(), any(), any());
    }
}