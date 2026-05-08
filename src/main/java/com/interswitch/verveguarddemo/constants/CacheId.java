package com.interswitch.verveguarddemo.constants;

import lombok.Getter;

@Getter
public enum CacheId {

    // Account
    ACCOUNT(Names.ACCOUNT),
    ACCOUNTS(Names.ACCOUNTS),
    ACCOUNT_BY_NUMBER(Names.ACCOUNT_BY_NUMBER),

    // Card
    CARD(Names.CARD),
    CARDS(Names.CARDS),
    CARD_ID_BY_HASH(Names.CARD_ID_BY_HASH),
    CARD_ACTIVE_BY_HASH(Names.CARD_ACTIVE_BY_HASH),

    // Merchant
    MERCHANT(Names.MERCHANT),
    MERCHANTS(Names.MERCHANTS),
    MERCHANTS_PAGE(Names.MERCHANTS_PAGE),
    MERCHANT_VALIDATION(Names.MERCHANT_VALIDATION),
    MERCHANT_ID_BY_USER(Names.MERCHANT_ID_BY_USER),
    MERCHANT_ROLE_ID(Names.MERCHANT_ROLE_ID),
    MERCHANT_TIER_EXISTS(Names.MERCHANT_TIER_EXISTS),
    MERCHANT_EMAIL(Names.MERCHANT_EMAIL),
    MERCHANT_EMAIL_BY_ACCOUNT(Names.MERCHANT_EMAIL_BY_ACCOUNT),
    MERCHANT_NAME_BY_ACCOUNT(Names.MERCHANT_NAME_BY_ACCOUNT),

    // Blacklist
    BLACKLIST(Names.BLACKLIST),
    BLACKLISTED_MERCHANT(Names.BLACKLISTED_MERCHANT),
    FRAUD_EVALUATION(Names.FRAUD_EVALUATION),

    // Tier
    TIER_CONFIG(Names.TIER_CONFIG),

    // User
    USER(Names.USER),
    USERS(Names.USERS),
    USERS_PAGE(Names.USERS_PAGE),
    USER_BY_EMAIL(Names.USER_BY_EMAIL),
    USER_ROLE_NAME(Names.USER_ROLE_NAME),

    // Roles & Permissions
    ROLE_PERMISSIONS(Names.ROLE_PERMISSIONS),
    PERMISSIONS(Names.PERMISSIONS),

    // Rate Limiting
    RATE_LIMIT(Names.RATE_LIMIT);

    private final String cacheName;

    CacheId(String cacheName) {
        this.cacheName = cacheName;
    }

    /**
     * String constants for use in annotations (which require compile-time constants).
     * Use CacheId enum for programmatic access; use Names for @Cacheable/@CacheEvict.
     */
    public static final class Names {
        public static final String ACCOUNT = "account";
        public static final String ACCOUNTS = "accounts";
        public static final String ACCOUNT_BY_NUMBER = "account-by-number";

        public static final String CARD = "card";
        public static final String CARDS = "cards";
        public static final String CARD_ID_BY_HASH = "card-id-by-hash";
        public static final String CARD_ACTIVE_BY_HASH = "card-active-by-hash";

        public static final String MERCHANT = "merchant";
        public static final String MERCHANTS = "merchants";
        public static final String MERCHANTS_PAGE = "merchants-page";
        public static final String MERCHANT_VALIDATION = "merchant-validation";
        public static final String MERCHANT_ID_BY_USER = "merchant-id-by-user";
        public static final String MERCHANT_ROLE_ID = "merchant-role-id";
        public static final String MERCHANT_TIER_EXISTS = "merchant-tier-exists";
        public static final String MERCHANT_EMAIL = "merchant-email";
        public static final String MERCHANT_EMAIL_BY_ACCOUNT = "merchant-email-by-account";
        public static final String MERCHANT_NAME_BY_ACCOUNT = "merchant-name-by-account";

        public static final String BLACKLIST = "blacklist";
        public static final String BLACKLISTED_MERCHANT = "blacklisted-merchant";
        public static final String FRAUD_EVALUATION = "fraud-eval";

        public static final String TIER_CONFIG = "tier-config";

        public static final String USER = "user";
        public static final String USERS = "users";
        public static final String USERS_PAGE = "users-page";
        public static final String USER_BY_EMAIL = "user-by-email";
        public static final String USER_ROLE_NAME = "user-role-name";

        public static final String ROLE_PERMISSIONS = "role-permissions";
        public static final String PERMISSIONS = "permissions";

        public static final String RATE_LIMIT = "rate-limit";

        private Names() {}
    }
}