package com.interswitch.verveguarddemo.controllers.v1;

import com.interswitch.verveguarddemo.models.request.LoginRequest;
import com.interswitch.verveguarddemo.models.response.AuthResponse;
import com.interswitch.verveguarddemo.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "Endpoints for identity management, session handling, and token lifecycle")
@RestController
@RequestMapping("auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String refreshTokenHeader = "Refresh-Token";
    private final AuthService authService;

    @Operation(
            summary = "Login",
            description = "Authenticates user credentials and returns access and refresh tokens. Public access."
    )
    @ResponseStatus(HttpStatus.OK)
    @PostMapping("login")
    public AuthResponse login(@RequestBody @Valid LoginRequest request) {
        return authService.login(request);
    }

    @Operation(
            summary = "Refresh Token",
            description = "Generates a new access token using a valid refresh token. Public access."
    )
    @ResponseStatus(HttpStatus.OK)
    @PostMapping("refresh")
    public AuthResponse refresh(@RequestHeader(refreshTokenHeader) String refreshToken) {
        return authService.refresh(refreshToken);
    }
}