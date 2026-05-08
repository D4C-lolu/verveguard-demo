-- =============================================================
--  USERS  (role_id 1 = SUPER_ADMIN, 2 = ADMIN, 3 = MERCHANT)
-- =============================================================
INSERT INTO users (firstname, lastname, email, phone,
				   password_hash, user_status, role_id,
				   created_at, updated_at)
VALUES ('Test', 'Admin', 'testadmin@verveguard.com', '11111111111',
		'$2a$10$mRxdXj7YESYMm/DIxn9X9OVXykdCww6ZEUX9a1jrOY2GildUkm4Ji', 'ACTIVE', 2, now(), now()),
	   ('Suspended', 'User', 'suspended@verveguard.com', '33333333333',
		'$2a$10$mRxdXj7YESYMm/DIxn9X9OVXykdCww6ZEUX9a1jrOY2GildUkm4Ji', 'SUSPENDED', 3, now(), now()),
	   ('Deleted', 'User', 'deleted@verveguard.com', '44444444444',
		'$2a$10$mRxdXj7YESYMm/DIxn9X9OVXykdCww6ZEUX9a1jrOY2GildUkm4Ji', 'INACTIVE', 3, now(), now());

-- =============================================================
--  MERCHANTS
-- =============================================================
INSERT INTO merchants (firstname, lastname, email, phone,
					   password_hash, role_id,
					   address, kyc_status, merchant_status, tier,
					   created_at, updated_at)
VALUES ('Test', 'Merchant', 'testmerchant@verveguard.com', '22222222222',
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

-- =============================================================
--  CARDS  (one per merchant, only merchant 1 needs an account)
-- =============================================================
INSERT INTO cards (merchant_id, card_number, card_type, scheme,
				   expiry_month, expiry_year, card_hash, card_status,
				   created_at, updated_at)
VALUES ((SELECT id FROM merchants WHERE email = 'testmerchant@verveguard.com'),
		'4111********1111', 'VIRTUAL', 'VERVE', 12, 2027,
		encode(digest('4111111111111111', 'sha256'), 'hex'),
		'ACTIVE', now(), now()),
	   ((SELECT id FROM merchants WHERE email = 'testmerchant2@verveguard.com'),
		'4222********2222', 'VIRTUAL', 'VERVE', 12, 2027,
		encode(digest('4222222222222222', 'sha256'), 'hex'),
		'ACTIVE', now(), now());

-- =============================================================
--  ACCOUNTS  (one per card)
-- =============================================================
INSERT INTO accounts (card_id, account_number, account_type,
					  currency, balance, account_status,
					  created_at)
VALUES ((SELECT id FROM cards WHERE card_hash = encode(digest('4111111111111111', 'sha256'), 'hex')),
		'1100000001', 'SETTLEMENT', 'NGN', 10000000.0000, 'ACTIVE', now()),
	   ((SELECT id FROM cards WHERE card_hash = encode(digest('4222222222222222', 'sha256'), 'hex')),
		'1200000000', 'SETTLEMENT', 'NGN', 5000000.0000, 'ACTIVE', now());

-- =============================================================
--  MERCHANT BLACKLIST  (merchant 2 — testmerchant2@verveguard.com)
-- =============================================================
INSERT INTO merchant_blacklist (merchant_id, reason, blacklisted_at)
VALUES ((SELECT id FROM merchants WHERE email = 'testmerchant2@verveguard.com'),
		'Fraudulent activity detected', now());

INSERT INTO merchants (firstname, lastname, email, phone,
					   password_hash, role_id,
					   address, kyc_status, merchant_status, tier,
					   created_at, updated_at)
VALUES ('Expired', 'Merchant', 'expiredcard.merchant@verveguard.com', '555555555555',
		'$2a$10$mRxdXj7YESYMm/DIxn9X9OVXykdCww6ZEUX9a1jrOY2GildUkm4Ji', 3,
		'6 Test Street, Lagos', 'APPROVED', 'ACTIVE', 'TIER_1', now(), now());

INSERT INTO cards (merchant_id, card_number, card_type, scheme,
				   expiry_month, expiry_year, card_hash, card_status,
				   created_at, updated_at)
VALUES ((SELECT id FROM merchants WHERE email = 'expiredcard.merchant@verveguard.com'),
		'4999********9999', 'VIRTUAL', 'VISA', 1, 2020,
		encode(digest('4999999999999999', 'sha256'), 'hex'),
		'EXPIRED', now(), now());

INSERT INTO accounts (card_id, account_number, account_type,
					  currency, balance, account_status,
					  created_at)
VALUES ((SELECT id FROM cards WHERE card_hash = encode(digest('4999999999999999', 'sha256'), 'hex')),
		'1100000002', 'SETTLEMENT', 'NGN', 5000.0000, 'ACTIVE', now());


-- a user with no merchant account (for conflict test)
INSERT INTO users (firstname, lastname, email, phone,
				   password_hash, user_status, role_id,
				   created_at, updated_at)
VALUES ('Clean', 'User', 'cleanuser@verveguard.com', '66666666666',
		'$2a$10$mRxdXj7YESYMm/DIxn9X9OVXykdCww6ZEUX9a1jrOY2GildUkm4Ji',
		'ACTIVE', 1, now(), now());