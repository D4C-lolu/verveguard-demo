-- V99__seed_test_data_2026_03_12.sql

-- test users
INSERT INTO users (
    firstname, lastname, email, phone,
    password_hash, user_status, role_id,
    created_at, updated_at
) VALUES
      ('Test', 'Admin', 'testadmin@verveguard.com', '11111111111',
       '$2a$10$mRxdXj7YESYMm/DIxn9X9OVXykdCww6ZEUX9a1jrOY2GildUkm4Ji', 'ACTIVE', 2, now(), now()),
      ('Suspended', 'User', 'suspended@verveguard.com', '33333333333',
       '$2a$10$mRxdXj7YESYMm/DIxn9X9OVXykdCww6ZEUX9a1jrOY2GildUkm4Ji', 'SUSPENDED', 3, now(), now()),
      ('Deleted', 'User', 'deleted@verveguard.com', '44444444444',
       '$2a$10$mRxdXj7YESYMm/DIxn9X9OVXykdCww6ZEUX9a1jrOY2GildUkm4Ji', 'INACTIVE', 3, now(), now());
INSERT INTO merchants (
    firstname, lastname, email, phone,
    password_hash, role_id,
    address, kyc_status, merchant_status, tier,
    created_at, updated_at
) VALUES
      ('Test', 'Merchant', 'testmerchant@verveguard.com', '22222222222',
       '$2a$10$mRxdXj7YESYMm/DIxn9X9OVXykdCww6ZEUX9a1jrOY2GildUkm4Ji', 3,
       '5 Test Street, Lagos', 'APPROVED', 'ACTIVE', 'TIER_1', now(), now()),

      ('Test', 'Merchant2', 'testmerchant2@verveguard.com', '444114444444',
       '$2a$10$mRxdXj7YESYMm/DIxn9X9OVXykdCww6ZEUX9a1jrOY2GildUkm4Ji', 3,
       '3 Test Street, Lagos', 'PENDING', 'ACTIVE', 'TIER_1', now(), now()),

      ('Test', 'Merchant3', 'testmerchant3@verveguard.com', '444114444445',
       '$2a$10$mRxdXj7YESYMm/DIxn9X9OVXykdCww6ZEUX9a1jrOY2GildUkm4Ji', 3,
       NULL, 'PENDING', 'ACTIVE', 'TIER_1', now(), now()),

      ('Test', 'Merchant4', 'testmerchant4@verveguard.com', '444114444446',
       '$2a$10$mRxdXj7YESYMm/DIxn9X9OVXykdCww6ZEUX9a1jrOY2GildUkm4Ji', 3,
       '4 Test Street, Lagos', 'APPROVED', 'ACTIVE', 'TIER_1', now(), now());


INSERT INTO accounts (
     merchant_id, account_number, account_type,
    currency, balance, ledger_balance, account_status,
    created_at, updated_at
) VALUES
      ( 1, '1100000001', 'SETTLEMENT', 'NGN', 10000000.0000, 10000000.0000, 'ACTIVE', now(), now()),
      ( 1, '1100000002', 'SETTLEMENT', 'NGN', 0.0000, 0.0000, 'ACTIVE', now(), now()),
      ( 2, '2200000001', 'SETTLEMENT', 'NGN', 10000000.0000, 10000000.0000, 'ACTIVE', now(), now());



-- blacklist merchant 2 (testmerchant@verveguard.com)
INSERT INTO public.merchant_blacklist (
    merchant_id, reason, blacklisted_at
) VALUES (
             2,
             'Fraudulent activity detected',
             now()
         );

-- third account belonging to merchant 1 (testadmin@verveguard.com)
INSERT INTO public.accounts (
    merchant_id, account_number, account_type,
    currency, balance, ledger_balance, account_status,
    created_at, updated_at
) VALUES (
             1,
             '1100000003',
             'SETTLEMENT',
             'NGN',
             0.0000,
             0.0000,
             'ACTIVE',
             now(), now()
         );