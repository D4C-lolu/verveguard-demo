package com.interswitch.verveguarddemo.constants;

public final class Permissions {

    public static final String USER_READ = "user:read";
    public static final String USER_CREATE = "user:create";
    public static final String USER_UPDATE = "user:update";
    public static final String USER_DELETE = "user:delete";


    // Merchant
    public static final String MERCHANT_READ = "merchant:read";
    public static final String MERCHANT_CREATE = "merchant:create";
    public static final String MERCHANT_UPDATE = "merchant:update";
    public static final String MERCHANT_DELETE = "merchant:delete";
    public static final String MERCHANT_KYC = "merchant:kyc";
    public static final String MERCHANT_BLACKLIST = "merchant:blacklist";

    // Transaction
    public static final String TRANSACTION_CREATE = "transaction:create";


    // Tier
    public static final String TIER_READ = "tier:read";
    public static final String TIER_UPDATE = "tier:update";

    // Role & Permission
    public static final String ROLE_READ = "role:read";
    public static final String ROLE_CREATE = "role:create";
    public static final String PERMISSION_READ = "permission:read";
    public static final String PERMISSION_ASSIGN = "permission:assign";

    // System
    public static final String SYSTEM_MONITOR = "system:monitor";

    private Permissions() {
    }
}