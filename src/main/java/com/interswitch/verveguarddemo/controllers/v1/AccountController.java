package com.interswitch.verveguarddemo.controllers.v1;

import com.interswitch.verveguarddemo.constants.Permissions;
import com.interswitch.verveguarddemo.constants.Roles;
import com.interswitch.verveguarddemo.models.response.AccountResponse;
import com.interswitch.verveguarddemo.services.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Accounts", description = "Account lookup endpoints")
@RestController
@RequestMapping("accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @Operation(
            summary = "Get my account (Merchant)",
            description = "Returns the authenticated merchant's account. Requires ACCOUNT_READ authority."
    )
    @GetMapping("me")
    @PreAuthorize("hasAuthority('" + Permissions.ACCOUNT_READ + "')")
    public AccountResponse getMyAccount() {
        return accountService.getMyAccount();
    }

    @Operation(
            summary = "Get account by card number (Admin)",
            description = "Look up any account by card number. Requires ADMIN or SUPER_ADMIN role."
    )
    @GetMapping("card/{cardNumber}")
    @PreAuthorize("hasRole('" + Roles.ADMIN + "') or hasRole('" + Roles.SUPER_ADMIN + "')")
    public AccountResponse getAccountByCardNumber(@PathVariable String cardNumber) {
        return accountService.getAccountByCardNumber(cardNumber);
    }

    @Operation(
            summary = "List all accounts (Admin)",
            description = "Paginated list of all accounts. Requires ADMIN or SUPER_ADMIN role."
    )
    @GetMapping
    @PreAuthorize("hasRole('" + Roles.ADMIN + "') or hasRole('" + Roles.SUPER_ADMIN + "')")
    public Page<AccountResponse> getAllAccounts(
            @RequestParam(defaultValue = "1")          int page,
            @RequestParam(defaultValue = "10")         int size,
            @RequestParam(defaultValue = "created_at") String sortField,
            @RequestParam(defaultValue = "DESC")       Sort.Direction sortDirection
    ) {
        return accountService.getAllAccounts(page, size, sortField, sortDirection);
    }
}