package com.interswitch.verveguarddemo.repositories;

import com.interswitch.verveguarddemo.entities.Merchant;
import com.interswitch.verveguarddemo.entities.User;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {

    @Cacheable(value = "merchant-by-email", key = "#email")
    @Query("SELECT m FROM Merchant m JOIN FETCH m.role r LEFT JOIN FETCH r.permissions WHERE m.email = :email")
    Optional<Merchant> findByEmail(String email);

}
