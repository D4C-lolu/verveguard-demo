package com.interswitch.verveguarddemo.dao;

import com.interswitch.verveguarddemo.models.enums.FraudStatus;
import com.interswitch.verveguarddemo.models.projections.FraudAttemptRecord;
import com.interswitch.verveguarddemo.models.projections.StaticFraudData;
import com.interswitch.verveguarddemo.models.response.FraudAttemptResponse;
import com.interswitch.verveguarddemo.models.response.MerchantInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Array;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FraudDao {

    private final NamedParameterJdbcTemplate namedJdbc;

    public StaticFraudData getEvaluationData(String cardHash) {
        return namedJdbc.queryForObject(
                "SELECT * FROM sp_fraud_get_evaluation_data(:cardHash)",
                new MapSqlParameterSource("cardHash", cardHash),
                (rs, _) -> new StaticFraudData(
                        rs.getBoolean("is_card_blocked"),
                        rs.getBoolean("is_merchant_blacklisted"),
                        rs.getBigDecimal("transaction_limit"),
                        rs.getLong("merchant_id")
                )
        );
    }

    public void insertFraudAttempt(FraudAttemptRecord record) {
        namedJdbc.update(
                "SELECT sp_fraud_insert_attempt(:cardHash, :merchantId, :ipAddress, " +
                        ":amount, :currency, :status, :flags)",
                new MapSqlParameterSource()
                        .addValue("cardHash",   record.cardHash())
                        .addValue("merchantId", record.merchantId())
                        .addValue("ipAddress",  record.ipAddress())
                        .addValue("amount",     record.amount())
                        .addValue("currency",   record.currency())
                        .addValue("status",     record.status().name())
                        .addValue("flags",      record.flags().toArray(new String[0]))
        );
    }

    // Fallback only — normally covered by getEvaluationData
    public int getCardVelocityCount(String cardHash, OffsetDateTime since) {
        Integer count = namedJdbc.queryForObject(
                "SELECT sp_fraud_get_card_velocity_count(:cardHash, :since)",
                new MapSqlParameterSource()
                        .addValue("cardHash", cardHash)
                        .addValue("since",    since),
                Integer.class
        );
        return count != null ? count : 0;
    }

    // Fallback only — normally covered by getEvaluationData
    public boolean isBlacklistedByCardNumber(String cardNumber) {
        return Boolean.TRUE.equals(namedJdbc.queryForObject(
                "SELECT sp_fraud_is_blacklisted_by_card_number(:cardNumber)",
                new MapSqlParameterSource("cardNumber", cardNumber),
                Boolean.class
        ));
    }

    // Fallback only — normally covered by getEvaluationData
    public Optional<BigDecimal> getTransactionLimitByCardNumber(String cardNumber) {
        BigDecimal limit = namedJdbc.queryForObject(
                "SELECT sp_fraud_get_transaction_limit_by_card_number(:cardNumber)",
                new MapSqlParameterSource("cardNumber", cardNumber),
                BigDecimal.class
        );
        return Optional.ofNullable(limit);
    }

    public Page<FraudAttemptResponse> getFraudAttempts(int page, int size) {
        long[] total = {0};

        List<FraudAttemptResponse> content = namedJdbc.query(
                "SELECT * FROM sp_fraud_get_attempts(:limit, :offset)",
                new MapSqlParameterSource()
                        .addValue("limit",  size)
                        .addValue("offset", (long) (page - 1) * size),
                (rs, rowNum) -> {
                    if (rowNum == 0) total[0] = rs.getLong("total_count");
                    return fraudAttemptRowMapper().mapRow(rs, rowNum);
                }
        );

        return new PageImpl<>(content, PageRequest.of(page - 1, size), total[0]);
    }
    private RowMapper<FraudAttemptResponse> fraudAttemptRowMapper() {
        return (rs, _) -> {
            Array flagsArray = rs.getArray("flags");
            List<String> flags = flagsArray != null
                    ? Arrays.asList((String[]) flagsArray.getArray())
                    : List.of();

            MerchantInfo merchant = new MerchantInfo(
                    rs.getString("merchant_firstname"),
                    rs.getString("merchant_lastname"),
                    rs.getString("merchant_othername"),
                    rs.getString("merchant_email"),
                    rs.getString("merchant_phone")
            );

            return new FraudAttemptResponse(
                    rs.getLong("id"),
                    rs.getString("card_hash"),
                    rs.getLong("merchant_id"),
                    merchant,
                    rs.getString("ip_address"),
                    rs.getBigDecimal("amount"),
                    rs.getString("currency"),
                    FraudStatus.valueOf(rs.getString("status")),
                    flags,
                    rs.getObject("created_at", OffsetDateTime.class)
            );
        };
    }
}