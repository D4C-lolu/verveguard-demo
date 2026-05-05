package com.interswitch.verveguarddemo.controllers.v1;

import com.interswitch.verveguarddemo.constants.Permissions;
import com.interswitch.verveguarddemo.models.enums.MerchantTier;
import com.interswitch.verveguarddemo.models.request.CreateTierConfigRequest;
import com.interswitch.verveguarddemo.models.request.UpdateTierConfigRequest;
import com.interswitch.verveguarddemo.models.response.TierConfigResponse;
import com.interswitch.verveguarddemo.services.TierConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Tier Configuration", description = "Endpoints for managing transaction limits and fees associated with different merchant tiers")
@RestController
@RequestMapping("tier-configs")
@RequiredArgsConstructor
public class TierConfigController {

    private final TierConfigService tierConfigService;

    @Operation(
            summary = "Create Tier Configuration",
            description = "Define new limits and fee structures for a specific tier level. Requires TIER_UPDATE authority."
    )
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @PreAuthorize("hasAuthority('" + Permissions.TIER_UPDATE + "')")
    public TierConfigResponse createTierConfig(@RequestBody @Valid CreateTierConfigRequest request) {
        return tierConfigService.createTierConfig(request);
    }

    @Operation(
            summary = "List All Tier Configs",
            description = "Retrieve all active tier configurations in the system. Requires TIER_READ authority."
    )
    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.TIER_READ + "')")
    public List<TierConfigResponse> getAllTierConfigs() {
        return tierConfigService.getAllTierConfigs();
    }

    @Operation(
            summary = "Get Tier Config by ID",
            description = "Fetch configuration details using the internal primary key. Requires TIER_READ authority."
    )
    @GetMapping("{tierConfigId}")
    @PreAuthorize("hasAuthority('" + Permissions.TIER_READ + "')")
    public TierConfigResponse getTierConfigById(@PathVariable Long tierConfigId) {
        return tierConfigService.getTierConfigById(tierConfigId);
    }

    @Operation(
            summary = "Get Config by Tier Name",
            description = "Lookup limits and fees for a specific MerchantTier (e.g., TIER_1, TIER_2). Requires TIER_READ authority."
    )
    @GetMapping("tier/{tier}")
    @PreAuthorize("hasAuthority('" + Permissions.TIER_READ + "')")
    public TierConfigResponse getTierConfigByTier(@PathVariable MerchantTier tier) {
        return tierConfigService.getTierConfigByTier(tier);
    }

    @Operation(
            summary = "Update Tier Configuration",
            description = "Modify limits or fee percentages for an existing tier. Requires TIER_UPDATE authority."
    )
    @PutMapping("{tier}")
    @PreAuthorize("hasAuthority('" + Permissions.TIER_UPDATE + "')")
    public TierConfigResponse updateTierConfig(
            @PathVariable MerchantTier tier,
            @RequestBody @Valid UpdateTierConfigRequest request
    ) {
        return tierConfigService.updateTierConfig(tier, request);
    }
}