package com.interswitch.verveguarddemo.services.auth;

import com.interswitch.verveguarddemo.base.BaseIntegrationTest;
import com.interswitch.verveguarddemo.exceptions.InvalidTokenException;
import com.interswitch.verveguarddemo.models.request.LoginRequest;
import com.interswitch.verveguarddemo.models.response.AuthResponse;
import com.interswitch.verveguarddemo.services.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Auth Refresh Integration Tests")
public class AuthRefreshIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AuthService authService;

    @Test
    @DisplayName("should refresh tokens successfully")
    void shouldRefreshTokensSuccessfully() {
        AuthResponse loginResponse = authService.login(
                new LoginRequest("testadmin@verveguard.com", "Admin123!"));

        AuthResponse refreshResponse = authService.refresh(loginResponse.refreshToken());

        assertThat(refreshResponse).isNotNull();
        assertThat(refreshResponse.accessToken()).isNotBlank();
        assertThat(refreshResponse.refreshToken()).isNotBlank();
        assertThat(refreshResponse.accessToken()).isNotEqualTo(loginResponse.accessToken());
        assertThat(refreshResponse.refreshToken()).isNotEqualTo(loginResponse.refreshToken());
    }

    @Test
    @DisplayName("should fail refresh with invalid token")
    void shouldFailRefreshWithInvalidToken() {
        assertThatThrownBy(() -> authService.refresh("invalid.token.here"))
                .isInstanceOf(InvalidTokenException.class);
    }
}