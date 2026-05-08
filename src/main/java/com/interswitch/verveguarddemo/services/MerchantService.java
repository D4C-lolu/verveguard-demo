package com.interswitch.verveguarddemo.services;

import com.interswitch.verveguarddemo.dao.BlacklistDao;
import com.interswitch.verveguarddemo.entities.Merchant;
import com.interswitch.verveguarddemo.entities.Role;
import com.interswitch.verveguarddemo.exceptions.BadRequestException;
import com.interswitch.verveguarddemo.exceptions.NotFoundException;
import com.interswitch.verveguarddemo.models.enums.KycStatus;
import com.interswitch.verveguarddemo.models.enums.MerchantStatus;
import com.interswitch.verveguarddemo.models.enums.MerchantTier;
import com.interswitch.verveguarddemo.models.request.ChangePasswordRequest;
import com.interswitch.verveguarddemo.models.request.CreateMerchantRequest;
import com.interswitch.verveguarddemo.models.response.MerchantResponse;
import com.interswitch.verveguarddemo.repositories.MerchantRepository;
import com.interswitch.verveguarddemo.util.SecurityUtil;
import com.interswitch.verveguarddemo.util.ValidationUtil;
import com.interswitch.verveguarddemo.constants.CacheId;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final PasswordEncoder passwordEncoder;
    private final BlacklistDao blacklistDao;

    @Transactional
    @CacheEvict(value = CacheId.Names.MERCHANTS_PAGE, allEntries = true)
    public MerchantResponse createMerchant(CreateMerchantRequest request) {
        List<Map<String, Object>> conflicts = merchantRepository.validateForCreate(request.email(), request.phone(), request.roleId());
        ValidationUtil.checkConflicts(conflicts);

        Long currentUserId = SecurityUtil.findCurrentUserId().orElse(null);

        Merchant merchant = Merchant.builder()
                .firstname(request.firstname())
                .lastname(request.lastname())
                .othername(request.othername())
                .email(request.email())
                .phone(request.phone())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(new Role(request.roleId()))
                .address(request.address())
                .kycStatus(KycStatus.PENDING)
                .merchantStatus(MerchantStatus.INACTIVE)
                .tier(MerchantTier.TIER_1)
                .build();

        merchant.setCreatedBy(currentUserId);
        merchantRepository.save(merchant);

        return MerchantResponse.builder()
                .id(merchant.getId())
                .firstname(merchant.getFirstname())
                .lastname(merchant.getLastname())
                .othername(merchant.getOthername())
                .email(merchant.getEmail())
                .phone(merchant.getPhone())
                .address(merchant.getAddress())
                .kycStatus(merchant.getKycStatus())
                .merchantStatus(merchant.getMerchantStatus())
                .tier(merchant.getTier())
                .createdAt(merchant.getCreatedAt())
                .updatedAt(merchant.getUpdatedAt())
                .build();
    }

    @Cacheable(value = CacheId.Names.MERCHANTS, key = "#id")
    public MerchantResponse getMerchantById(Long id) {
        return merchantRepository.findMerchantById(id)
                .orElseThrow(() -> new NotFoundException("Merchant not found"));
    }

    @Cacheable(value = CacheId.Names.MERCHANTS_PAGE, key = "#status + '-' + #page + '-' + #size + '-' + #sort + '-' + #dir")
    public Page<MerchantResponse> getMerchantsByStatus(MerchantStatus status, int page, int size, String sort, Sort.Direction dir) {
        return merchantRepository.findByMerchantStatus(status, PageRequest.of(page - 1, size, Sort.by(dir, sort)));
    }

    @Cacheable(value = CacheId.Names.MERCHANTS_PAGE, key = "#status + '-' + #page + '-' + #size + '-' + #sort + '-' + #dir")
    public Page<MerchantResponse> getMerchantsByKycStatus(KycStatus status, int page, int size, String sort, Sort.Direction dir) {
        return merchantRepository.findByKycStatus(status, PageRequest.of(page - 1, size, Sort.by(dir, sort)));
    }

    @Transactional
    @CacheEvict(value = CacheId.Names.MERCHANTS, key = "#id")
    public void updatePassword(Long id, ChangePasswordRequest request) {
        String currentHash = merchantRepository.findPasswordHashById(id);
        if (currentHash == null || !passwordEncoder.matches(request.currentPassword(), currentHash)) {
            throw new BadRequestException("Current password incorrect");
        }
        merchantRepository.updatePassword(id, passwordEncoder.encode(request.newPassword()), SecurityUtil.getCurrentUserId());
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheId.Names.MERCHANTS, key = "#id"),
            @CacheEvict(value = CacheId.Names.MERCHANTS_PAGE, allEntries = true)
    })
    public void updateKycStatus(Long id, KycStatus kycStatus) {
        merchantRepository.updateKycStatus(id, kycStatus, SecurityUtil.getCurrentUserId());
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheId.Names.MERCHANTS, key = "#id"),
            @CacheEvict(value = CacheId.Names.MERCHANTS_PAGE, allEntries = true)
    })
    public void blacklistMerchant(Long id, String reason) {
        blacklistDao.blacklistMerchant(id, reason, SecurityUtil.getCurrentUserId());
    }


    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheId.Names.MERCHANTS, key = "#id"),
            @CacheEvict(value = CacheId.Names.MERCHANTS_PAGE, allEntries = true)
    })
    public void updateMerchantStatus(Long id, MerchantStatus merchantStatus) {
        merchantRepository.updateMerchantStatus(id, merchantStatus, SecurityUtil.getCurrentUserId());
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheId.Names.MERCHANTS, key = "#id"),
            @CacheEvict(value = CacheId.Names.MERCHANTS_PAGE, allEntries = true)
    })
    public void updateMerchantStatusAndKycStatus(Long id, MerchantStatus merchantStatus, KycStatus kycStatus) {
        merchantRepository.updateMerchantStatusAndKycStatus(id, merchantStatus, kycStatus, SecurityUtil.getCurrentUserId());
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheId.Names.MERCHANTS, key = "#id"),
            @CacheEvict(value = CacheId.Names.MERCHANTS_PAGE, allEntries = true)
    })
    public void deleteMerchant(Long id) {
        merchantRepository.softDelete(id, SecurityUtil.getCurrentUserId());
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheId.Names.MERCHANTS, key = "#id"),
            @CacheEvict(value = CacheId.Names.MERCHANTS_PAGE, allEntries = true)
    })
    public void upgradeTier(Long id) {
        MerchantTier existingTier = merchantRepository.findMerchantTierById(id)
                .orElseThrow(() -> new NotFoundException("Merchant not found"));
        MerchantTier nextTier = switch (existingTier) {
            case TIER_1 -> MerchantTier.TIER_2;
            case TIER_2 -> MerchantTier.TIER_3;
            case TIER_3 -> throw new BadRequestException("Already at highest tier");
        };
        merchantRepository.updateTier(id, nextTier, SecurityUtil.getCurrentUserId());
    }
}