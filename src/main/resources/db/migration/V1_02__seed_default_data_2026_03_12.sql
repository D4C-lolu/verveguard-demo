-- roles
INSERT INTO roles (name, principal_type)
VALUES ('SUPER_ADMIN', 'ADMIN'),
	   ('ADMIN', 'ADMIN'),
	   ('MERCHANT', 'MERCHANT');

INSERT INTO permissions (name, description)
VALUES
	-- user management
	('user:read', 'View users'),
	('user:create', 'Create users'),
	('user:update', 'Update user details'),
	('user:delete', 'Delete users'),
	
	-- merchant management
	('merchant:read', 'View merchants'),
	('merchant:create', 'Create/onboard merchants'),
	('merchant:update', 'Update merchant details'),
	('merchant:delete', 'Delete merchants'),
	('merchant:kyc', 'Manage merchant KYC'),
	('merchant:blacklist', 'Blacklist merchants'),
	
	-- transaction management
	('transaction:create', 'Create transactions'),
	
	-- tier management
	('tier:read', 'View tier config'),
	('tier:update', 'Update tier config'),
	
	-- role & permission management
	('role:read', 'View roles'),
	('role:create', 'Create roles'),
	('permission:read', 'View permissions'),
	('permission:assign', 'Assign permissions to roles'),
	
	-- system
	('system:monitor', 'Access actuator endpoints');
-- SUPER_ADMIN gets everything
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE name = 'SUPER_ADMIN'), id
FROM permissions;

-- ADMIN gets everything except role/permission management and destructive ops
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE name = 'ADMIN'), id
FROM permissions
WHERE name NOT IN (
				   'role:create',
				   'permission:assign',
				   'merchant:blacklist',
				   'merchant:delete'
	);

-- super admin user
INSERT INTO users (firstname, lastname, email, phone,
				   password_hash, user_status, role_id,
				   created_at, updated_at)
VALUES ('Super', 'Admin',
		'superadmin@verveguard.com',
		'00000000000',
		'$2a$10$mRxdXj7YESYMm/DIxn9X9OVXykdCww6ZEUX9a1jrOY2GildUkm4Ji',
		'ACTIVE',
		(SELECT id FROM roles WHERE name = 'SUPER_ADMIN'),
		now(), now());

INSERT INTO tier_configs (tier,
						  daily_transaction_limit,
						  single_transaction_limit,
						  monthly_transaction_limit,
						  created_at, updated_at)
VALUES ('TIER_1', 100000.0000, 10000.0000, 1000000.0000, now(), now()),
	   ('TIER_2', 500000.0000, 50000.0000, 5000000.0000, now(), now()),
	   ('TIER_3', 2000000.0000, 200000.0000, 20000000.0000, now(), now());

-- demo merchant
INSERT INTO merchants (firstname, lastname, email, phone,
					   password_hash, role_id,
					   address, kyc_status, merchant_status, tier,
					   created_at, updated_at)
VALUES ('Demo', 'Merchant',
		'demo.merchant@verveguard.com',
		'08000000000',
		'$2a$10$mRxdXj7YESYMm/DIxn9X9OVXykdCww6ZEUX9a1jrOY2GildUkm4Ji',
		(SELECT id FROM roles WHERE name = 'MERCHANT'),
		'1 Demo Street, Lagos',
		'APPROVED', 'ACTIVE', 'TIER_2',
		now(), now());

-- demo card
INSERT INTO cards (merchant_id, card_number, card_hash,
				   card_type, scheme,
				   expiry_month, expiry_year, card_status,
				   created_at, updated_at)
VALUES ((SELECT id FROM merchants WHERE email = 'demo.merchant@verveguard.com'),
		'4011********1111',
		'5e81b4df99b1a00af009d61f9bfb04c03af02364ad6d269a7cfb0fab1fd06181',
		'VIRTUAL', 'VISA',
		12, 2028, 'ACTIVE',
		now(), now());

-- demo account with funds
INSERT INTO accounts (card_id, account_number, account_type,
					  currency, balance, account_status,
					  created_at)
VALUES ((SELECT id FROM cards WHERE card_hash = '5e81b4df99b1a00af009d61f9bfb04c03af02364ad6d269a7cfb0fab1fd06181'),
		'0000000000',
		'SETTLEMENT', 'NGN',
		500000000.0000, 'ACTIVE',
		now());