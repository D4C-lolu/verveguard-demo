package com.interswitch.verveguarddemo.controllers.v1;


import com.interswitch.verveguarddemo.constants.Permissions;
import com.interswitch.verveguarddemo.constants.Roles;
import com.interswitch.verveguarddemo.models.request.*;
import com.interswitch.verveguarddemo.models.response.AccountResponse;
import com.interswitch.verveguarddemo.services.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Account Management", description = "Authenticated endpoints for managing merchant balances and account status")
@RestController
@RequestMapping("accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @Operation(summary = "Create account (Admin)",
            description = "Requires ADMIN or SUPER_ADMIN role. Creates a new account for a specific merchant.")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @PreAuthorize("hasRole('"+ Roles.ADMIN +"') or hasRole('" + Roles.SUPER_ADMIN + "')")
    public AccountResponse createAccount(@RequestBody @Valid CreateAccountRequest request) {
        return accountService.createAccount(request);
    }

    @Operation(summary = "Create self-account (Merchant)",
            description = "Requires MERCHANT role. Allows a merchant to open their own account.")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("me")
    @PreAuthorize("hasRole('"+ Roles.MERCHANT +"')")
    public AccountResponse createAccountForSelf(@RequestBody @Valid CreateMyAccountRequest request) {
        return accountService.createAccountForSelf(request);
    }

    @Operation(summary = "Get account details",
            description = "Requires ACCOUNT_READ authority. Fetches balance and status by ID.")
    @GetMapping("{accountId}")
    @PreAuthorize("hasAuthority('" + Permissions.ACCOUNT_READ + "')")
    public AccountResponse getAccountById(@PathVariable Long accountId) {
        return accountService.getAccountById(accountId);
    }

    @Operation(summary = "List accounts by Merchant",
            description = "Requires ACCOUNT_READ authority. Returns a paginated list of accounts.")
    @GetMapping("merchant/{merchantId}")
    @PreAuthorize("hasAuthority('" + Permissions.ACCOUNT_READ + "')")
    public Page<AccountResponse> getAccountsByMerchant(
            @PathVariable Long merchantId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "created_at") String sortField,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection
    ) {
        return accountService.getAccountsByMerchant(merchantId, page, size, sortField, sortDirection);
    }
}