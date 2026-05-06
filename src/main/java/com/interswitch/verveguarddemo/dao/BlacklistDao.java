package com.interswitch.verveguarddemo.dao;

import com.github.benmanes.caffeine.cache.Cache;
import com.interswitch.verveguarddemo.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BlacklistDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final Cache<Long, Boolean> blacklistedMerchantCache;

    public void blacklistMerchant(Long merchantId, String reason, Long blacklistedBy) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("merchantId", merchantId)
                .addValue("reason", reason)
                .addValue("blacklistedBy", blacklistedBy);

        jdbcTemplate.update(
                "SELECT blacklist_merchant(:merchantId, :reason, :blacklistedBy)",
                params
        );
        blacklistedMerchantCache.put(merchantId, true);
    }

    public boolean blockByHash(String cardHash) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("cardHash", cardHash)
                .addValue("updatedBy", SecurityUtil.getCurrentUserId());

        Boolean result = jdbcTemplate.queryForObject(
                "SELECT sp_card_block_by_hash(:cardHash, :updatedBy)",
                params,
                Boolean.class
        );
        return Boolean.TRUE.equals(result);
    }

    public boolean isBlacklisted(Long merchantId) {
        Boolean cached = blacklistedMerchantCache.getIfPresent(merchantId);
        if (cached != null) return cached;

        MapSqlParameterSource params = new MapSqlParameterSource("merchantId", merchantId);

        Boolean result = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM merchant_blacklist WHERE merchant_id = :merchantId AND lifted_at IS NULL)",
                params,
                Boolean.class
        );
        boolean blacklisted = Boolean.TRUE.equals(result);
        blacklistedMerchantCache.put(merchantId, blacklisted);
        return blacklisted;
    }
}