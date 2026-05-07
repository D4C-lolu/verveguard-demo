package com.interswitch.verveguarddemo.security;

import com.interswitch.verveguarddemo.entities.Merchant;
import com.interswitch.verveguarddemo.entities.User;
import com.interswitch.verveguarddemo.models.enums.PrincipalType;
import com.interswitch.verveguarddemo.repositories.MerchantRepository;
import com.interswitch.verveguarddemo.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final MerchantRepository merchantRepository;

    @Override
    public @NonNull UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            return new UserPrincipal(user.get());
        }

        Optional<Merchant> merchant = merchantRepository.findByEmail(email);
        if (merchant.isPresent()) {
            return new MerchantPrincipal(merchant.get());
        }

        throw new UsernameNotFoundException("No account found for email: " + email);
    }

    public UserDetails loadUserById(Long id, PrincipalType principalType) {
        return switch (principalType) {
            case ADMIN -> userRepository.findById(id)
                    .map(UserPrincipal::new)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + id));
            case MERCHANT -> merchantRepository.findById(id)
                    .map(MerchantPrincipal::new)
                    .orElseThrow(() -> new UsernameNotFoundException("Merchant not found with ID: " + id));
        };
    }
}