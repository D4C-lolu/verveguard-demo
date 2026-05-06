package com.interswitch.verveguarddemo.dao;

import com.interswitch.verveguarddemo.models.enums.CardScheme;
import com.interswitch.verveguarddemo.models.enums.CardStatus;
import com.interswitch.verveguarddemo.models.enums.CardType;
import com.interswitch.verveguarddemo.models.projections.CardValidationResult;
import com.interswitch.verveguarddemo.models.request.CreateCardRequest;
import com.interswitch.verveguarddemo.models.response.CardResponse;
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
public class CardDao {

    private final NamedParameterJdbcTemplate namedJdbc;

    public Long insert(Long merchantId, String maskedCardNumber, String cardHash,
                       CreateCardRequest request, Long createdBy) {
        return namedJdbc.queryForObject(
                "SELECT sp_card_insert(:merchantId, :cardNumber, :cardHash, :cardType, :scheme, " +
                        ":expiryMonth::smallint, :expiryYear::smallint, :cardStatus, :createdBy)",
                new MapSqlParameterSource()
                        .addValue("merchantId",  merchantId)
                        .addValue("cardNumber",  maskedCardNumber)
                        .addValue("cardHash",    cardHash)
                        .addValue("cardType",    request.cardType().name())
                        .addValue("scheme",      request.scheme().name())
                        .addValue("expiryMonth", request.expiryMonth())
                        .addValue("expiryYear",  request.expiryYear())
                        .addValue("cardStatus",  CardStatus.ACTIVE.name())
                        .addValue("createdBy",   createdBy),
                Long.class);
    }

    public Optional<CardResponse> findByMerchantId(Long merchantId) {
        return namedJdbc.query(
                "SELECT * FROM sp_card_find_by_merchant(:merchantId)",
                new MapSqlParameterSource("merchantId", merchantId),
                cardRowMapper()
        ).stream().findFirst();
    }

    public Optional<CardResponse> findByCardNumber(String cardNumber) {
        return namedJdbc.query(
                "SELECT * FROM sp_card_find_by_card_number(:cardNumber)",
                new MapSqlParameterSource("cardNumber", cardNumber),
                cardRowMapper()
        ).stream().findFirst();
    }

    public Page<CardResponse> findAll(int page, int size, String sortField, String sortDir) {
        long[] total = {0};

        List<CardResponse> content = namedJdbc.query(
                "SELECT * FROM sp_card_find_all(:limit, :offset, :sortField, :sortDir)",
                new MapSqlParameterSource()
                        .addValue("limit",     size)
                        .addValue("offset",    (long) (page - 1) * size)
                        .addValue("sortField", sortField)
                        .addValue("sortDir",   sortDir),
                (rs, rowNum) -> {
                    if (rowNum == 0) total[0] = rs.getLong("total_count");
                    return cardRowMapper().mapRow(rs, rowNum);
                });

        return new PageImpl<>(content, PageRequest.of(page - 1, size), total[0]);
    }

    public Optional<CardValidationResult> getCardCreationValidation(Long merchantId, String cardHash) {
        return namedJdbc.query(
                "SELECT * FROM sp_card_get_creation_validation(:merchantId, :cardHash)",
                new MapSqlParameterSource()
                        .addValue("merchantId", merchantId)
                        .addValue("cardHash",   cardHash),
                (rs, _) -> new CardValidationResult(
                        rs.getString("kyc_status"),
                        rs.getBoolean("card_hash_exists"),
                        rs.getBoolean("already_has_card")
                )
        ).stream().findFirst();
    }

    public boolean exists(Long id) {
        return Boolean.TRUE.equals(namedJdbc.queryForObject(
                "SELECT sp_card_exists(:id)",
                new MapSqlParameterSource("id", id),
                Boolean.class));
    }

    public void blockCard(Long id, Long updatedBy) {
        namedJdbc.update(
                "SELECT sp_card_block(:id, :updatedBy)",
                new MapSqlParameterSource()
                        .addValue("id",        id)
                        .addValue("updatedBy", updatedBy));
    }

    public void expireDueCards() {
        namedJdbc.update("SELECT sp_card_expire_due()", new MapSqlParameterSource());
    }

    private RowMapper<CardResponse> cardRowMapper() {
        return (rs, _) -> new CardResponse(
                rs.getLong("id"),
                rs.getLong("merchant_id"),
                rs.getString("card_number"),
                CardType.valueOf(rs.getString("card_type")),
                CardScheme.valueOf(rs.getString("scheme")),
                rs.getInt("expiry_month"),
                rs.getInt("expiry_year"),
                CardStatus.valueOf(rs.getString("card_status")),
                rs.getString("account_number"),
                rs.getString("account_type"),
                rs.getString("currency"),
                rs.getBigDecimal("balance"),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class));
    }
}