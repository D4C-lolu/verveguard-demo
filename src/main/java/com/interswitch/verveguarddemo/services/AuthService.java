package com.interswitch.verveguarddemo.services;

import com.interswitch.verveguarddemo.models.request.LoginRequest;
import com.interswitch.verveguarddemo.models.response.AuthResponse;
import com.interswitch.verveguarddemo.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public AuthResponse login(LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return tokenService.issueTokens(principal);
    }

    public AuthResponse refresh(String refreshToken) {
        return tokenService.refresh(refreshToken);
    }

}