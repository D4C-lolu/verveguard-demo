package com.interswitch.verveguarddemo.services;

import com.interswitch.verveguarddemo.configuration.JwtProperties;
import com.interswitch.verveguarddemo.entities.Permission;
import com.interswitch.verveguarddemo.exceptions.UnauthorizedException;
import com.interswitch.verveguarddemo.models.enums.PrincipalType;
import com.interswitch.verveguarddemo.security.AdminPrincipal;
import com.interswitch.verveguarddemo.security.JwtUserPrincipal;
import com.interswitch.verveguarddemo.security.MerchantPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    private SecretKey signingKey;

    @PostConstruct
    private void init() {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.getSecret()));
    }

    private SecretKey getSigningKey() {
        return signingKey;
    }

    public String generateAccessToken(UserDetails principal) {
        return generateToken(principal, jwtProperties.getAccessTokenExpiry());
    }

    public String generateRefreshToken(UserDetails principal) {
        return generateToken(principal, jwtProperties.getRefreshTokenExpiry());
    }

    public PrincipalType extractPrincipalType(String token) {
        String type = extractAllClaims(token).get("principalType", String.class);
        return PrincipalType.valueOf(type);
    }

    private String generateToken(UserDetails principal, long expiry) {
        String subject;
        String email;
        String role;
        List<String> permissions;
        PrincipalType principalType;

        switch (principal) {
            case AdminPrincipal p -> {
                subject = String.valueOf(p.getId());
                email = p.user().getEmail();
                role = p.user().getRole().getName();
                permissions = p.user().getRole().getPermissions().stream()
                        .map(Permission::getName)
                        .toList();
                principalType = PrincipalType.ADMIN;
            }
            case MerchantPrincipal p -> {
                subject = String.valueOf(p.getId());
                email = p.merchant().getEmail();
                role = p.merchant().getRole().getName();
                permissions =  p.merchant().getRole().getPermissions().stream()
                        .map(Permission::getName)
                        .toList();
                principalType = PrincipalType.MERCHANT;
            }
            default -> throw new UnauthorizedException("Invalid user type");
        }

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(subject)
                .claim("email", email)
                .claim("role", role)
                .claim("permissions", permissions)
                .claim("principalType", principalType.name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + (expiry * 1000)))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long extractUserId(String token) {
        return  Long.parseLong(extractAllClaims(token).getSubject());
    }

    public String extractJti(String token) {
        return extractAllClaims(token).getId();
    }

    public Date extractIssuedAt(String token) {
        return extractAllClaims(token).getIssuedAt();
    }

    public boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    public boolean isTokenInvalid(String token) {
        try {
            return isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return true;
        }
    }

    @SuppressWarnings("unchecked")
    public JwtUserPrincipal extractUserPrincipal(String token) {
        Claims claims = extractAllClaims(token);
        return new JwtUserPrincipal(
                Long.valueOf(claims.getSubject()),
                claims.get("email", String.class),
                claims.get("role", String.class),
                claims.get("permissions", List.class),
                PrincipalType.valueOf(claims.get("principalType", String.class))
        );
    }
}
