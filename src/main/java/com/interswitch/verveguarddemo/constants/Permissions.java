package com.interswitch.verveguarddemo.constants;
public final class Permissions {

    // Merchant
    public static final String MERCHANT_READ      = "merchant:read";
    public static final String MERCHANT_CREATE    = "merchant:create";
    public static final String MERCHANT_UPDATE    = "merchant:update";
    public static final String MERCHANT_DELETE    = "merchant:delete";
    public static final String MERCHANT_KYC       = "merchant:kyc";
    public static final String MERCHANT_BLACKLIST = "merchant:blacklist";

    // Account
    public static final String ACCOUNT_READ       = "account:read";
    public static final String ACCOUNT_CREATE     = "account:create";

    // Card
    public static final String CARD_READ          = "card:read";
    public static final String CARD_CREATE        = "card:create";
    public static final String CARD_BLOCK         = "card:block";
    public static final String CARD_UNBLOCK       = "card:unblock";

    // Transaction
    public static final String TRANSACTION_READ    = "transaction:read";
    public static final String TRANSACTION_CREATE  = "transaction:create";
    public static final String TRANSACTION_REVERSE = "transaction:reverse";

    // Transfer
    public static final String TRANSFER_READ       = "transfer:read";
    public static final String TRANSFER_CREATE     = "transfer:create";
    public static final String TRANSFER_REVERSE    = "transfer:reverse";

    // Tier
    public static final String TIER_READ           = "tier:read";
    public static final String TIER_UPDATE         = "tier:update";

    // Role & Permission
    public static final String ROLE_READ           = "role:read";
    public static final String ROLE_CREATE         = "role:create";
    public static final String ROLE_UPDATE         = "role:update";
    public static final String ROLE_DELETE         = "role:delete";
    public static final String PERMISSION_READ     = "permission:read";
    public static final String PERMISSION_ASSIGN   = "permission:assign";

    // System
    public static final String SYSTEM_MONITOR      = "system:monitor";

    private Permissions() {}
}