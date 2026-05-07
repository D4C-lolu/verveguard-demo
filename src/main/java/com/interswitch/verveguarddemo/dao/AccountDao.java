package com.interswitch.verveguarddemo.dao;

import com.interswitch.verveguarddemo.models.enums.AccountType;
import com.interswitch.verveguarddemo.models.response.AccountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AccountDao {

    private final NamedParameterJdbcTemplate namedJdbc;

    public Optional<AccountResponse> findByMerchantId(Long merchantId) {
        return namedJdbc.query(
                "SELECT * FROM sp_account_find_by_merchant(:merchantId)",
                new MapSqlParameterSource("merchantId", merchantId),
                accountRowMapper()
        ).stream().findFirst();
    }

    public Optional<AccountResponse> findByCardNumber(String cardNumber) {
        return namedJdbc.query(
                "SELECT * FROM sp_account_find_by_card_number(:cardNumber)",
                new MapSqlParameterSource("cardNumber", cardNumber),
                accountRowMapper()
        ).stream().findFirst();
    }

    public void createForCard(Long cardId) {
        namedJdbc.query(
                "SELECT sp_account_create_for_card(:cardId)",
                new MapSqlParameterSource("cardId", cardId),
                (_) -> {
                }
        );
    }

    public Page<AccountResponse> findAll(int page, int size, String sortField, String sortDir) {
        long[] total = {0};

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("limit", size)
                .addValue("offset", (long) (page - 1) * size)
                .addValue("sortField", sortField)
                .addValue("sortDir", sortDir);

        List<AccountResponse> content = namedJdbc.query(
                "SELECT * FROM sp_account_find_all(:limit, :offset, :sortField, :sortDir)",
                params,
                (rs, rowNum) -> {
                    if (rowNum == 0) total[0] = rs.getLong("total_count");
                    return accountRowMapper().mapRow(rs, rowNum);
                });

        return new PageImpl<>(content, PageRequest.of(page - 1, size), total[0]);
    }

    private RowMapper<AccountResponse> accountRowMapper() {
        return (rs, _) -> new AccountResponse(
                rs.getLong("id"),
                rs.getLong("merchant_id"),
                rs.getString("account_number"),
                AccountType.valueOf(rs.getString("account_type")),
                rs.getString("currency"),
                rs.getBigDecimal("balance"),
                rs.getObject("created_at", OffsetDateTime.class)
        );
    }
}