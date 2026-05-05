package com.interswitch.verveguarddemo.repositories;

import com.interswitch.verveguarddemo.entities.TierConfig;
import com.interswitch.verveguarddemo.models.enums.MerchantTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TierConfigRepository extends JpaRepository<TierConfig, Long> {

    Optional<TierConfig> findByTier(MerchantTier tier);

    boolean existsByTier(MerchantTier tier);
}
