package com.interswitch.verveguarddemo.services;


import com.interswitch.verveguarddemo.dao.AccountDao;
import com.interswitch.verveguarddemo.exceptions.BadRequestException;
import com.interswitch.verveguarddemo.exceptions.NotFoundException;
import com.interswitch.verveguarddemo.models.response.AccountResponse;
import com.interswitch.verveguarddemo.util.SecurityUtil;
import com.interswitch.verveguarddemo.constants.CacheId;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AccountService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "account_number", "account_type", "currency",
            "balance", "created_at"
    );
    private final AccountDao accountDao;

    @Cacheable(value = CacheId.Names.ACCOUNT, key = "'merchant:' + #merchantId")
    public AccountResponse getMyAccount() {
        Long merchantId = SecurityUtil.getCurrentUserId();
        return accountDao.findByMerchantId(merchantId)
                .orElseThrow(() -> new NotFoundException("Account not found"));
    }

    @Cacheable(value = CacheId.Names.ACCOUNT, key = "'card:' + #cardNumber")
    public AccountResponse getAccountByCardNumber(String cardNumber) {
        return accountDao.findByCardNumber(cardNumber)
                .orElseThrow(() -> new NotFoundException("Account not found"));
    }

    @Cacheable(value = CacheId.Names.ACCOUNTS, key = "'all:' + #page + ':' + #size + ':' + #sortField + ':' + #direction")
    public Page<AccountResponse> getAllAccounts(int page, int size, String sortField, Sort.Direction direction) {
        if (!ALLOWED_SORT_FIELDS.contains(sortField))
            throw new BadRequestException("Invalid sort field: " + sortField);

        return accountDao.findAll(page, size, sortField, direction.name());
    }
}