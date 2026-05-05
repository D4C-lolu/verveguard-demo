package com.interswitch.verveguarddemo.dao;

import com.interswitch.verveguarddemo.models.enums.KycStatus;
import com.interswitch.verveguarddemo.models.enums.MerchantStatus;
import com.interswitch.verveguarddemo.models.enums.MerchantTier;
import com.interswitch.verveguarddemo.models.enums.UserStatus;
import com.interswitch.verveguarddemo.models.projections.MerchantValidationResult;
import com.interswitch.verveguarddemo.models.response.MerchantResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MerchantDao {

    private final NamedParameterJdbcTemplate namedJdbc;

    @Cacheable(value = "merchant", key = "#id")
    public Optional<MerchantResponse> findById(Long id) {
        return namedJdbc.query(
                "SELECT * FROM sp_merchant_find_by_id(:id)",
                new MapSqlParameterSource("id", id),
                merchantRowMapper()
        ).stream().findFirst();
    }

    public Page<MerchantResponse> findAll(int page, int size, String sortField, Sort.Direction sortDirection) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("sortField", sortField)
                .addValue("sortDir", sortDirection.name())
                .addValue("limit", size)
                .addValue("offset", (long) (page - 1) * size);

        return queryPage(
                "SELECT * FROM sp_merchant_find_all(:sortField, :sortDir, :limit, :offset)",
                params, page, size
        );
    }

    public Page<MerchantResponse> findByStatus(MerchantStatus status, int page, int size, String sortField, Sort.Direction sortDirection) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("status", status.name())
                .addValue("sortField", sortField)
                .addValue("sortDir", sortDirection.name())
                .addValue("limit", size)
                .addValue("offset", (long) (page - 1) * size);

        return queryPage(
                "SELECT * FROM sp_merchant_find_by_status(:status, :sortField, :sortDir, :limit, :offset)",
                params, page, size
        );
    }

    public Page<MerchantResponse> findByKycStatus(KycStatus kycStatus, int page, int size, String sortField, Sort.Direction sortDirection) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("kycStatus", kycStatus.name())
                .addValue("sortField", sortField)
                .addValue("sortDir", sortDirection.name())
                .addValue("limit", size)
                .addValue("offset", (long) (page - 1) * size);

        return queryPage(
                "SELECT * FROM sp_merchant_find_by_kyc_status(:kycStatus, :sortField, :sortDir, :limit, :offset)",
                params, page, size
        );
    }

    private Page<MerchantResponse> queryPage(String sql, MapSqlParameterSource params, int page, int size) {
        long[] totalHolder = {0L};

        List<MerchantResponse> content = namedJdbc.query(sql, params, (rs, rowNum) -> {
            if (rowNum == 0) totalHolder[0] = rs.getLong("total_count");
            return merchantRowMapper().mapRow(rs, rowNum);
        });

        return new PageImpl<>(content, PageRequest.of(page - 1, size), totalHolder[0]);
    }

    public Long insert(MapSqlParameterSource params) {
        return namedJdbc.queryForObject(
                "SELECT sp_merchant_insert(:userId, :address, :kycStatus, :merchantStatus, :tier, :createdBy)",
                params, Long.class
        );
    }

    public MerchantValidationResult validateMerchantCreation(Long userId, String tier) {
        return namedJdbc.queryForObject(
                "SELECT * FROM sp_merchant_validate_creation(:userId, :tier)",
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("tier", tier),
                (rs, _) -> new MerchantValidationResult(
                        rs.getBoolean("merchant_exists"),
                        rs.getBoolean("user_exists"),
                        rs.getBoolean("tier_exists")
                ));
    }

    @Cacheable(value = "merchant-role-id", key = "#name")
    public Optional<Long> findRoleIdByName(String name) {
        Long result = namedJdbc.queryForObject(
                "SELECT sp_merchant_find_role_id_by_name(:name)",
                new MapSqlParameterSource("name", name),
                Long.class
        );
        return Optional.ofNullable(result);
    }

    @Cacheable(value = "merchant-tier-exists", key = "#tier")
    public boolean tierDoesNotExist(String tier) {
        return !Boolean.TRUE.equals(namedJdbc.queryForObject(
                "SELECT sp_merchant_tier_exists(:tier)",
                new MapSqlParameterSource("tier", tier),
                Boolean.class
        ));
    }

    public boolean exists(Long id) {
        return Boolean.TRUE.equals(namedJdbc.queryForObject(
                "SELECT sp_merchant_exists(:id)",
                new MapSqlParameterSource("id", id),
                Boolean.class
        ));
    }

    @CacheEvict(value = "merchant", key = "#id")
    public void updateAddress(Long id, String address, Long updatedBy) {
        namedJdbc.queryForList(
                "SELECT sp_merchant_update_address(:id, :address, :updatedBy)",
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("address", address)
                        .addValue("updatedBy", updatedBy)
        );
    }

    @CacheEvict(value = "merchant", key = "#id")
    public void updateKycStatus(Long id, String kycStatus, Long updatedBy) {
        namedJdbc.queryForList(
                "SELECT sp_merchant_update_kyc_status(:id, :kycStatus, :updatedBy)",
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("kycStatus", kycStatus)
                        .addValue("updatedBy", updatedBy)
        );
    }

    @CacheEvict(value = "merchant", key = "#id")
    public void updateMerchantStatus(Long id, String merchantStatus, Long updatedBy) {
        namedJdbc.queryForList(
                "SELECT sp_merchant_update_status(:id, :merchantStatus, :updatedBy)",
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("merchantStatus", merchantStatus)
                        .addValue("updatedBy", updatedBy)
        );
    }

    @CacheEvict(value = "merchant", key = "#id")
    public void updateMerchantStatusAndKycStatus(Long id, String merchantStatus, String kycStatus, Long updatedBy) {
        namedJdbc.queryForList(
                "SELECT sp_merchant_update_status_and_kyc(:id, :merchantStatus, :kycStatus, :updatedBy)",
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("merchantStatus", merchantStatus)
                        .addValue("kycStatus", kycStatus)
                        .addValue("updatedBy", updatedBy)
        );
    }

    @CacheEvict(value = "merchant", key = "#id")
    public void updateTier(Long id, String tier, Long updatedBy) {
        namedJdbc.queryForList(
                "SELECT sp_merchant_update_tier(:id, :tier, :updatedBy)",
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("tier", tier)
                        .addValue("updatedBy", updatedBy)
        );
    }

    @CacheEvict(value = "merchant", key = "#id")
    public void softDelete(Long id, Long deletedBy) {
        namedJdbc.queryForList(
                "SELECT sp_merchant_soft_delete(:id, :deletedBy)",
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("deletedBy", deletedBy)
        );
    }

    @Cacheable(value = ".verveguarddemo.-by-account", key = "#accountNumber")
    public Optional<String> getEmailByAccountNumber(String accountNumber) {
        String result = namedJdbc.queryForObject(
                "SELECT sp_merchant_get_email_by_account(:accountNumber)",
                new MapSqlParameterSource("accountNumber", accountNumber),
                String.class
        );
        return Optional.ofNullable(result);
    }

    @Cacheable(value = "merchant-name-by-account", key = "#accountNumber")
    public Optional<String> getNameByAccountNumber(String accountNumber) {
        String result = namedJdbc.queryForObject(
                "SELECT sp_merchant_get_name_by_account(:accountNumber)",
                new MapSqlParameterSource("accountNumber", accountNumber),
                String.class
        );
        return Optional.ofNullable(result);
    }

    private RowMapper<MerchantResponse> merchantRowMapper() {
        return (rs, _) -> new MerchantResponse(
                rs.getLong("id"),
                rs.getString("address"),
                KycStatus.valueOf(rs.getString("kyc_status")),
                MerchantStatus.valueOf(rs.getString("merchant_status")),
                MerchantTier.valueOf(rs.getString("tier")),
                rs.getLong("user_id"),
                rs.getString("firstname"),
                rs.getString("lastname"),
                rs.getString("email"),
                rs.getString("phone"),
                UserStatus.valueOf(rs.getString("user_status")),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class)
        );
    }
}