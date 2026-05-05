package com.interswitch.verveguarddemo.dao;

import com.interswitch.verveguarddemo.models.projections.BalanceMismatch;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MaintenanceDao {

    private final NamedParameterJdbcTemplate namedJdbc;

    public int bulkResolveSuccessfulTransfers() {
        Integer count = namedJdbc.queryForObject(
                "SELECT sp_maintenance_bulk_resolve_successful_transfers()",
                new MapSqlParameterSource(),
                Integer.class
        );
        return count != null ? count : 0;
    }

    public int bulkResolveFailedTransfers() {
        Integer count = namedJdbc.queryForObject(
                "SELECT sp_maintenance_bulk_resolve_failed_transfers()",
                new MapSqlParameterSource(),
                Integer.class
        );
        return count != null ? count : 0;
    }

    public List<BalanceMismatch> findBalanceMismatches() {
        return namedJdbc.query(
                "SELECT * FROM sp_maintenance_find_balance_mismatches()",
                new MapSqlParameterSource(),
                (rs, _) -> new BalanceMismatch(
                        rs.getLong("id"),
                        rs.getBigDecimal("stored_balance"),
                        rs.getBigDecimal("calculated_balance")
                )
        );
    }

    public void recalculateAllBalances() {
        namedJdbc.queryForList(
                "SELECT sp_maintenance_recalculate_all_balances()",
                new MapSqlParameterSource()
        );
    }
}
