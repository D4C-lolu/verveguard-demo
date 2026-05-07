package com.interswitch.verveguarddemo.services;


import com.interswitch.verveguarddemo.base.BaseIntegrationTest;
import com.interswitch.verveguarddemo.exceptions.BadRequestException;
import com.interswitch.verveguarddemo.exceptions.NotFoundException;
import com.interswitch.verveguarddemo.models.response.AccountResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@DisplayName("Account Service Integration Tests")
public class AccountServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        authenticateAsUser("superadmin@verveguard.com");
    }

    // -------------------------------------------------------------------------
    // getMyAccount
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should get my account successfully")
    void shouldGetMyAccountSuccessfully() {
        authenticateAsMerchant("demo.merchant@verveguard.com"); // has seeded account

        AccountResponse response = accountService.getMyAccount();

        assertThat(response).isNotNull();
        assertThat(response.balance()).isEqualByComparingTo(new BigDecimal("500000000.0000"));
    }

    @Test
    @DisplayName("should fail get my account when merchant has no account")
    void shouldFailGetMyAccountWhenMerchantHasNoAccount() {
        authenticateAsMerchant("testmerchant4@verveguard.com"); // no account seeded

        assertThatThrownBy(() -> accountService.getMyAccount())
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Account not found");
    }

    // -------------------------------------------------------------------------
    // getAccountByCardNumber
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should get account by card number successfully")
    void shouldGetAccountByCardNumberSuccessfully() {
        // masked number as stored for demo.merchant's card
        AccountResponse response = accountService.getAccountByCardNumber("4011111111111111");

        assertThat(response).isNotNull();
        assertThat(response.balance()).isEqualByComparingTo(new BigDecimal("500000000.0000"));
    }

    @Test
    @DisplayName("should fail get account by card number when not found")
    void shouldFailGetAccountByCardNumberWhenNotFound() {
        assertThatThrownBy(() -> accountService.getAccountByCardNumber("9999****9999"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Account not found");
    }

    // -------------------------------------------------------------------------
    // getAllAccounts
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should get all accounts paginated")
    void shouldGetAllAccountsPaginated() {
        Page<AccountResponse> page = accountService.getAllAccounts(1, 10, "created_at", Sort.Direction.DESC);

        assertThat(page.getContent()).isNotEmpty();
    }

    @Test
    @DisplayName("should fail get all accounts with invalid sort field")
    void shouldFailGetAllAccountsWithInvalidSortField() {
        assertThatThrownBy(() -> accountService.getAllAccounts(1, 10, "invalid_field", Sort.Direction.ASC))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid sort field");
    }
}