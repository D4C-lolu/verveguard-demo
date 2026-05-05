package com.interswitch.verveguarddemo.services;

import com.interswitch.verveguarddemo.dao.UserDao;
import com.interswitch.verveguarddemo.exceptions.InvalidTokenException;
import com.interswitch.verveguarddemo.models.enums.PrincipalType;
import com.interswitch.verveguarddemo.models.response.AuthResponse;
import com.interswitch.verveguarddemo.security.UserDetailsServiceImpl;
import com.interswitch.verveguarddemo.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    public AuthResponse issueTokens(UserPrincipal principal) {
        String accessToken  = jwtService.generateAccessToken(principal);
        String refreshToken = jwtService.generateRefreshToken(principal);
        return new AuthResponse(accessToken, refreshToken);
    }

    public AuthResponse refresh(String refreshToken) {
        if (jwtService.isTokenInvalid(refreshToken)) {
            throw new InvalidTokenException("Refresh token is invalid or expired");
        }

        Long id = jwtService.extractUserId(refreshToken);
        PrincipalType principalType = jwtService.extractPrincipalType(refreshToken);

        UserDetails principal = userDetailsService.loadUserById(id, principalType);

        return switch (principal) {
            case AdminPrincipal p    -> issueTokens(p);
            case MerchantPrincipal p -> issueTokens(p);
            default -> throw new InvalidTokenException("Unsupported principal type");
        };
    }

}
