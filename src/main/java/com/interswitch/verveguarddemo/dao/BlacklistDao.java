package com.interswitch.verveguarddemo.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BlacklistDao {

    private final NamedParameterJdbcTemplate namedJdbc;

    @Cacheable(value = "blacklist", key = "#merchantId")
    public boolean isActivelyBlacklisted(Long merchantId) {
        return Boolean.TRUE.equals(namedJdbc.queryForObject(
                "SELECT sp_blacklist_is_actively_blacklisted(:merchantId)",
                new MapSqlParameterSource("merchantId", merchantId),
                Boolean.class
        ));
    }
}
