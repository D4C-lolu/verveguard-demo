package com.interswitch.verveguarddemo.repositories;

import com.interswitch.verveguarddemo.entities.Merchant;
import com.interswitch.verveguarddemo.models.enums.KycStatus;
import com.interswitch.verveguarddemo.models.enums.MerchantStatus;
import com.interswitch.verveguarddemo.models.enums.MerchantTier;
import com.interswitch.verveguarddemo.models.response.MerchantResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Map;
import java.util.Optional;


public interface MerchantRepository extends JpaRepository<Merchant, Long>, JpaSpecificationExecutor<Merchant> {

    @Query("SELECT m FROM Merchant m JOIN FETCH m.role r LEFT JOIN FETCH r.permissions WHERE m.email = :email")
    Optional<Merchant> findByEmail(String email);

    @Query("SELECT new com.interswitch.verveguarddemo.models.response.MerchantResponse(m.id, m.address, m.kycStatus, m.merchantStatus, m.tier, m.firstname, m.lastname, m.email, m.phone, m.userStatus, m.createdAt, m.updatedAt) FROM Merchant m WHERE m.id = :id")
    Optional<MerchantResponse> findMerchantById(Long id);

    @Query("SELECT m.tier FROM Merchant m where m.id = :merchantId")
    Optional<MerchantTier> findMerchantTierById(Long merchantId);

    @Query("SELECT m.id AS id, (m.email = :email) AS email_exists, (m.phone = :phone) AS phone_exists FROM Merchant m WHERE m.email = :email OR m.phone = :phone")
    List<Map<String, Object>> validateForCreate(String email, String phone);

    @Query("SELECT m.passwordHash FROM Merchant m WHERE m.id = :id")
    String findPasswordHashById(Long id);

    @Modifying
    @Query("UPDATE Merchant m SET m.passwordHash = :passwordHash, m.updatedBy = :updatedBy WHERE m.id = :id")
    void updatePassword(Long id, String passwordHash, Long updatedBy);

    @Modifying
    @Query("UPDATE Merchant m SET m.kycStatus = :kycStatus, m.updatedBy = :updatedBy WHERE m.id = :id")
    void updateKycStatus(Long id, KycStatus kycStatus, Long updatedBy);

    @Modifying
    @Query("UPDATE Merchant m SET m.merchantStatus = :merchantStatus, m.updatedBy = :updatedBy WHERE m.id = :id")
    void updateMerchantStatus(Long id, MerchantStatus merchantStatus, Long updatedBy);

    @Modifying
    @Query("UPDATE Merchant m SET m.merchantStatus = :merchantStatus, m.kycStatus = :kycStatus, m.updatedBy = :updatedBy WHERE m.id = :id")
    void updateMerchantStatusAndKycStatus(Long id, MerchantStatus merchantStatus, KycStatus kycStatus, Long updatedBy);

    @Modifying
    @Query("UPDATE Merchant m SET m.tier = :tier, m.updatedBy = :updatedBy WHERE m.id = :id")
    void updateTier(Long id, MerchantTier tier, Long updatedBy);

    @Query("""
                SELECT new com.interswitch.verveguarddemo.models.response.MerchantResponse(
                    m.id, m.address, m.kycStatus, m.merchantStatus, m.tier, 
                    m.firstname, m.lastname, m.email, m.phone, m.userStatus, 
                    m.createdAt, m.updatedAt
                ) 
                FROM Merchant m 
                WHERE m.merchantStatus = :merchantStatus
            """)
    Page<MerchantResponse> findByMerchantStatus(MerchantStatus merchantStatus, Pageable pageable);

    @Query("""
                SELECT new com.interswitch.verveguarddemo.models.response.MerchantResponse(
                    m.id, m.address, m.kycStatus, m.merchantStatus, m.tier, 
                    m.firstname, m.lastname, m.email, m.phone, m.userStatus, 
                    m.createdAt, m.updatedAt
                ) 
                FROM Merchant m 
                WHERE m.kycStatus = :kycStatus
            """)
    Page<MerchantResponse> findByKycStatus(KycStatus kycStatus, Pageable pageable);
}
