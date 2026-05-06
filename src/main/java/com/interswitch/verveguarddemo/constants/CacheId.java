package com.interswitch.verveguarddemo.constants;

import lombok.Getter;

@Getter
public enum CacheId {

    // Account
    ACCOUNT("account"),
    ACCOUNT_BY_NUMBER("account-by-number"),

    // Card
    CARD("card"),
    CARD_ID_BY_HASH("card-id-by-hash"),
    CARD_ACTIVE_BY_HASH("card-active-by-hash"),

    // Merchant
    MERCHANT("merchant"),
    MERCHANT_VALIDATION("merchant-validation"),
    MERCHANT_ID_BY_USER("merchant-id-by-user"),
    MERCHANT_ROLE_ID("merchant-role-id"),
    MERCHANT_TIER_EXISTS("merchant-tier-exists"),
    MERCHANT_EMAIL("merchant-email"),
    MERCHANT_EMAIL_BY_ACCOUNT("merchant-email-by-account"),
    MERCHANT_NAME_BY_ACCOUNT("merchant-name-by-account"),

    // Blacklist
    BLACKLIST("blacklist"),
    BLACKLISTED_MERCHANT("blacklisted-merchant"),
    FRAUD_EVALUATION("fraud-eval"),

    // Tier
    TIER_CONFIG("tier-config"),

    // User
    USER("user"),
    USER_BY_EMAIL("user-by-email"),
    USER_ROLE_NAME("user-role-name"),

    // Roles & Permissions
    ROLE_PERMISSIONS("role-permissions"),
    PERMISSIONS("permissions");

    private final String cacheName;

    CacheId(String cacheName) {
        this.cacheName = cacheName;
    }
}