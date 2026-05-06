package com.interswitch.verveguarddemo.controllers.v1;

import com.interswitch.verveguarddemo.constants.Permissions;
import com.interswitch.verveguarddemo.models.enums.KycStatus;
import com.interswitch.verveguarddemo.models.enums.MerchantStatus;
import com.interswitch.verveguarddemo.models.request.CreateMerchantRequest;
import com.interswitch.verveguarddemo.models.response.MerchantResponse;
import com.interswitch.verveguarddemo.services.MerchantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Merchant Management", description = "Endpoints for merchant lifecycle, KYC verification, and tier management")
@RestController
@RequestMapping("merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    @Operation(summary = "Create Merchant (Admin)")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @PreAuthorize("hasAuthority('" + Permissions.MERCHANT_CREATE + "')")
    public MerchantResponse createMerchant(@RequestBody @Valid CreateMerchantRequest request) {
        return merchantService.createMerchant(request);
    }

    @Operation(summary = "List All Merchants")
    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.MERCHANT_READ + "')")
    public Page<MerchantResponse> getAllMerchants(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") Sort.Direction dir
    ) {
        // Pass individual params or PageRequest depending on your specific Service signature
        return merchantService.getMerchantsByStatus(null, page, size, sort, dir);
    }

    @Operation(summary = "Filter by Status")
    @GetMapping("status")
    @PreAuthorize("hasAuthority('" + Permissions.MERCHANT_READ + "')")
    public Page<MerchantResponse> getMerchantsByStatus(
            @RequestParam MerchantStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") Sort.Direction dir
    ) {
        return merchantService.getMerchantsByStatus(status, page, size, sort, dir);
    }

    @Operation(summary = "Filter by KYC Status")
    @GetMapping("kyc-status")
    @PreAuthorize("hasAuthority('" + Permissions.MERCHANT_READ + "')")
    public Page<MerchantResponse> getMerchantsByKycStatus(
            @RequestParam KycStatus kycStatus,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") Sort.Direction dir
    ) {
        return merchantService.getMerchantsByKycStatus(kycStatus, page, size, sort, dir);
    }

    @Operation(summary = "Get Merchant by ID")
    @GetMapping("{merchantId}")
    @PreAuthorize("hasAuthority('" + Permissions.MERCHANT_READ + "')")
    public MerchantResponse getMerchantById(@PathVariable Long merchantId) {
        return merchantService.getMerchantById(merchantId);
    }

    @Operation(summary = "Update Account Status")
    @PatchMapping("{merchantId}/status")
    @PreAuthorize("hasAuthority('" + Permissions.MERCHANT_UPDATE + "')")
    public void updateMerchantStatus(
            @PathVariable Long merchantId,
            @RequestParam MerchantStatus status
    ) {
        // Service method is now @Modifying/void for efficiency
        merchantService.updateMerchantStatus(merchantId, status);
    }

    @Operation(summary = "Update KYC Level")
    @PatchMapping("{merchantId}/kyc")
    @PreAuthorize("hasAuthority('" + Permissions.MERCHANT_KYC + "')")
    public void updateKycStatus(
            @PathVariable Long merchantId,
            @RequestParam KycStatus kycStatus
    ) {
        merchantService.updateKycStatus(merchantId, kycStatus);
    }

    @Operation(summary = "Upgrade Tier")
    @PatchMapping("{merchantId}/tier/upgrade")
    @PreAuthorize("hasAuthority('" + Permissions.MERCHANT_UPDATE + "')")
    public void upgradeTier(@PathVariable Long merchantId) {
        merchantService.upgradeTier(merchantId);
    }

    @Operation(summary = "Delete Merchant")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("{merchantId}")
    @PreAuthorize("hasAuthority('" + Permissions.MERCHANT_DELETE + "')")
    public void deleteMerchant(@PathVariable Long merchantId) {
        merchantService.deleteMerchant(merchantId);
    }
}