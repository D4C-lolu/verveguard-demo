package com.interswitch.verveguarddemo.security;

import com.interswitch.verveguarddemo.entities.Merchant;
import com.interswitch.verveguarddemo.models.enums.MerchantStatus;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public record MerchantPrincipal(Merchant merchant) implements UserDetails {

    public Long getId() {
        return merchant.getId();
    }

    @Override
    public @NonNull String getUsername() {
        return merchant.getEmail();
    }

    @Override
    public String getPassword() {
        return merchant.getPasswordHash();
    }

    @Override
    public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + merchant.getRole().getName()));
        merchant.getRole().getPermissions().forEach(p ->
                authorities.add(new SimpleGrantedAuthority(p.getName()))
        );
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() {
        return merchant.isNotDeleted() && merchant.getMerchantStatus() != MerchantStatus.SUSPENDED;
    }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() {
        return merchant.isNotDeleted() && merchant.getMerchantStatus() == MerchantStatus.ACTIVE;
    }
}