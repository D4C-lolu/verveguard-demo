package com.interswitch.verveguarddemo.security;

import com.interswitch.verveguarddemo.models.enums.PrincipalType;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * Lightweight UserDetails implementation built from JWT claims.
 * No database access required.
 */
public record JwtUserPrincipal(
        Long id,
        String email,
        String role,
        List<String> permissions,
        PrincipalType principalType
) implements UserDetails {

    @Override
    public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
        return Stream.concat(
                Stream.of(new SimpleGrantedAuthority("ROLE_" + role)),
                permissions.stream().map(SimpleGrantedAuthority::new)
        ).toList();
    }

    @Override
    public String getPassword() { return null; }

    @Override
    public @NonNull String getUsername() { return email; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}