package com.interswitch.verveguarddemo.services;

import com.interswitch.verveguarddemo.base.BaseIntegrationTest;
import com.interswitch.verveguarddemo.constants.Roles;
import com.interswitch.verveguarddemo.entities.Merchant;
import com.interswitch.verveguarddemo.entities.Role;
import com.interswitch.verveguarddemo.entities.User;
import com.interswitch.verveguarddemo.exceptions.BadRequestException;
import com.interswitch.verveguarddemo.exceptions.ConflictException;
import com.interswitch.verveguarddemo.exceptions.NotFoundException;
import com.interswitch.verveguarddemo.models.enums.KycStatus;
import com.interswitch.verveguarddemo.models.enums.MerchantStatus;
import com.interswitch.verveguarddemo.models.enums.MerchantTier;
import com.interswitch.verveguarddemo.models.request.ChangePasswordRequest;
import com.interswitch.verveguarddemo.models.request.CreateMerchantRequest;
import com.interswitch.verveguarddemo.models.response.MerchantResponse;
import com.interswitch.verveguarddemo.repositories.MerchantRepository;
import com.interswitch.verveguarddemo.repositories.RoleRepository;
import com.interswitch.verveguarddemo.repositories.UserRepository;
import com.interswitch.verveguarddemo.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Merchant Service Integration Tests")
public class MerchantServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MerchantService merchantService;
    @Autowired
    private MerchantRepository merchantRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void setUp() {
        User superAdmin = userRepository.findByEmail("superadmin@verveguard.com").orElseThrow();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new UserPrincipal(superAdmin), null, List.of())
        );
    }

    private CreateMerchantRequest buildCreateRequest(String email, String phone) {
        Long roleId = roleRepository.findByName(Roles.MERCHANT).map(Role::getId)
                .orElseThrow();
        return new CreateMerchantRequest(
                "Test", "Merchant", null,
                email, phone,
                "Admin123!",
                roleId,
                "1 New Street, Lagos"
        );
    }

    // -------------------------------------------------------------------------
    // createMerchant
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should create merchant successfully")
    void shouldCreateMerchantSuccessfully() {
        CreateMerchantRequest request = buildCreateRequest("newmerchant@test.com", "77777777777");

        MerchantResponse response = merchantService.createMerchant(request);

        assertThat(response.id()).isNotNull().isPositive();
        assertThat(response.email()).isEqualTo("newmerchant@test.com");
        assertThat(response.address()).isEqualTo("1 New Street, Lagos");
        assertThat(response.kycStatus()).isEqualTo(KycStatus.PENDING);
        assertThat(response.merchantStatus()).isEqualTo(MerchantStatus.INACTIVE);
        assertThat(response.tier()).isEqualTo(MerchantTier.TIER_1);
    }

    @Test
    @DisplayName("should fail create merchant with duplicate email")
    void shouldFailCreateMerchantWithDuplicateEmail() {
        assertThatThrownBy(() -> merchantService.createMerchant(
                buildCreateRequest("testmerchant@verveguard.com", "99999999999")
        ))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("should fail create merchant with duplicate phone")
    void shouldFailCreateMerchantWithDuplicatePhone() {
        assertThatThrownBy(() -> merchantService.createMerchant(
                buildCreateRequest("unique@test.com", "22222222222") // phone seeded under testmerchant
        ))
                .isInstanceOf(ConflictException.class);
    }

    // -------------------------------------------------------------------------
    // getMerchantById
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should get merchant by id successfully")
    void shouldGetMerchantByIdSuccessfully() {
        Merchant merchant = merchantRepository.findByEmail("demo.merchant@verveguard.com").orElseThrow();

        MerchantResponse response = merchantService.getMerchantById(merchant.getId());

        assertThat(response.id()).isEqualTo(merchant.getId());
        assertThat(response.email()).isEqualTo("demo.merchant@verveguard.com");
    }

    @Test
    @DisplayName("should fail get merchant with non existent id")
    void shouldFailGetMerchantWithNonExistentId() {
        assertThatThrownBy(() -> merchantService.getMerchantById(Long.MAX_VALUE))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Merchant not found");
    }

    // -------------------------------------------------------------------------
    // getMerchantsByStatus / getMerchantsByKycStatus
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should get merchants by merchant status paginated")
    void shouldGetMerchantsByMerchantStatusPaginated() {
        Page<MerchantResponse> page = merchantService.getMerchantsByStatus(
                MerchantStatus.ACTIVE, 1, 10, "createdAt", Sort.Direction.DESC
        );

        assertThat(page.getContent()).isNotEmpty();
        assertThat(page.getContent()).allSatisfy(m ->
                assertThat(m.merchantStatus()).isEqualTo(MerchantStatus.ACTIVE)
        );
    }

    @Test
    @DisplayName("should get merchants by kyc status paginated")
    void shouldGetMerchantsByKycStatusPaginated() {
        Page<MerchantResponse> page = merchantService.getMerchantsByKycStatus(
                KycStatus.PENDING, 1, 10, "createdAt", Sort.Direction.DESC
        );

        assertThat(page.getContent()).isNotEmpty();
        assertThat(page.getContent()).allSatisfy(m ->
                assertThat(m.kycStatus()).isEqualTo(KycStatus.PENDING)
        );
    }

    // -------------------------------------------------------------------------
    // updateKycStatus
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should update kyc status successfully")
    void shouldUpdateKycStatusSuccessfully() {
        Merchant merchant = merchantRepository.findByEmail("testmerchant2@verveguard.com").orElseThrow(); // PENDING

        assertThatNoException().isThrownBy(() ->
                merchantService.updateKycStatus(merchant.getId(), KycStatus.APPROVED)
        );

        MerchantResponse updated = merchantService.getMerchantById(merchant.getId());
        assertThat(updated.kycStatus()).isEqualTo(KycStatus.APPROVED);
    }

    // -------------------------------------------------------------------------
    // updateMerchantStatus
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should update merchant status successfully")
    void shouldUpdateMerchantStatusSuccessfully() {
        Merchant merchant = merchantRepository.findByEmail("demo.merchant@verveguard.com").orElseThrow(); // ACTIVE

        assertThatNoException().isThrownBy(() ->
                merchantService.updateMerchantStatus(merchant.getId(), MerchantStatus.SUSPENDED)
        );

        MerchantResponse updated = merchantService.getMerchantById(merchant.getId());
        assertThat(updated.merchantStatus()).isEqualTo(MerchantStatus.SUSPENDED);
    }

    // -------------------------------------------------------------------------
    // upgradeTier
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should upgrade tier successfully")
    void shouldUpgradeTierSuccessfully() {
        Merchant merchant = merchantRepository.findByEmail("testmerchant@verveguard.com").orElseThrow(); // TIER_1

        assertThatNoException().isThrownBy(() -> merchantService.upgradeTier(merchant.getId()));

        MerchantResponse updated = merchantService.getMerchantById(merchant.getId());
        assertThat(updated.tier()).isEqualTo(MerchantTier.TIER_2);
    }

    @Test
    @DisplayName("should fail upgrade tier when merchant is already on highest tier")
    void shouldFailUpgradeTierWhenAlreadyOnHighestTier() {
        Merchant merchant = merchantRepository.findByEmail("testmerchant@verveguard.com").orElseThrow();
        merchantService.upgradeTier(merchant.getId()); // TIER_1 -> TIER_2
        merchantService.upgradeTier(merchant.getId()); // TIER_2 -> TIER_3

        assertThatThrownBy(() -> merchantService.upgradeTier(merchant.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Already at highest tier");
    }

    @Test
    @DisplayName("should fail upgrade tier with non existent merchant")
    void shouldFailUpgradeTierWithNonExistentMerchant() {
        assertThatThrownBy(() -> merchantService.upgradeTier(Long.MAX_VALUE))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Merchant not found");
    }

    // -------------------------------------------------------------------------
    // blacklistMerchant
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should blacklist merchant successfully")
    void shouldBlacklistMerchantSuccessfully() {
        Merchant merchant = merchantRepository.findByEmail("testmerchant4@verveguard.com").orElseThrow();

        assertThatNoException().isThrownBy(() ->
                merchantService.blacklistMerchant(merchant.getId(), "Fraudulent activity")
        );
    }

    // -------------------------------------------------------------------------
    // updatePassword
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should update password successfully")
    void shouldUpdatePasswordSuccessfully() {
        Merchant merchant = merchantRepository.findByEmail("testmerchant4@verveguard.com").orElseThrow();
        ChangePasswordRequest request = new ChangePasswordRequest("Admin123!", "NewPassword456!", "NewPassword456!");

        assertThatNoException().isThrownBy(() ->
                merchantService.updatePassword(merchant.getId(), request)
        );
    }

    @Test
    @DisplayName("should fail update password with incorrect current password")
    void shouldFailUpdatePasswordWithIncorrectCurrentPassword() {
        Merchant merchant = merchantRepository.findByEmail("testmerchant4@verveguard.com").orElseThrow();
        ChangePasswordRequest request = new ChangePasswordRequest("WrongPassword!", "NewPassword456!", "NewPassword456!");

        assertThatThrownBy(() -> merchantService.updatePassword(merchant.getId(), request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Current password incorrect");
    }

    // -------------------------------------------------------------------------
    // deleteMerchant
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should soft delete merchant successfully")
    void shouldSoftDeleteMerchantSuccessfully() {
        MerchantResponse created = merchantService.createMerchant(
                buildCreateRequest("todelete@test.com", "88888888888")
        );

        assertThatNoException().isThrownBy(() -> merchantService.deleteMerchant(created.id()));

        assertThatThrownBy(() -> merchantService.getMerchantById(created.id()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Merchant not found");
    }
}