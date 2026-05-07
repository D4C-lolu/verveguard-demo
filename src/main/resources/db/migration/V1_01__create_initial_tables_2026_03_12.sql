CREATE
EXTENSION IF NOT EXISTS pgcrypto;

-- =============================================================
--  SEQUENCE
-- =============================================================
CREATE SEQUENCE IF NOT EXISTS account_number_seq
    START WITH 1000000000
    INCREMENT BY 1
    NO CYCLE;

-- =============================================================
--  ROLES
-- =============================================================
CREATE TABLE IF NOT EXISTS roles
(
	id
	bigint
	GENERATED
	BY
	DEFAULT AS
	IDENTITY,
	name
	character
	varying
(
	50
) NOT NULL,
	principal_type character varying
(
	20
) NOT NULL,
	created_at timestamp with time zone NOT NULL DEFAULT now(),
	created_by bigint,
	CONSTRAINT roles_pkey PRIMARY KEY
(
	id
),
	CONSTRAINT roles_name_unique UNIQUE
(
	name
),
	CONSTRAINT roles_principal_type_check CHECK
(
	principal_type
	IN
(
	'ADMIN',
	'MERCHANT'
))
	);

-- =============================================================
--  PERMISSIONS
-- =============================================================
CREATE TABLE IF NOT EXISTS permissions
(
	id
	bigint
	GENERATED
	BY
	DEFAULT AS
	IDENTITY,
	name
	character
	varying
(
	100
) NOT NULL,
	description text,
	CONSTRAINT permissions_pkey PRIMARY KEY
(
	id
),
	CONSTRAINT permissions_name_unique UNIQUE
(
	name
)
	);

-- =============================================================
--  USERS
-- =============================================================
CREATE TABLE IF NOT EXISTS users
(
	id
	bigint
	GENERATED
	BY
	DEFAULT AS
	IDENTITY,
	firstname
	character
	varying
(
	255
) NOT NULL,
	lastname character varying
(
	255
) NOT NULL,
	othername character varying
(
	255
),
	email character varying
(
	255
) NOT NULL,
	phone character varying
(
	20
) NOT NULL,
	password_hash character varying
(
	255
) NOT NULL,
	user_status character varying
(
	50
) NOT NULL,
	role_id bigint NOT NULL,
	created_at timestamp with time zone NOT NULL DEFAULT now(),
	updated_at timestamp with time zone NOT NULL,
	deleted_at timestamp
						 with time zone DEFAULT NULL,
							 created_by bigint,
							 updated_by bigint,
							 deleted_by bigint,
							 CONSTRAINT users_pkey PRIMARY KEY (id),
	CONSTRAINT users_email_unique UNIQUE
(
	email
),
	CONSTRAINT users_phone_unique UNIQUE
(
	phone
),
	CONSTRAINT users_role_fkey FOREIGN KEY
(
	role_id
) REFERENCES roles
(
	id
)
						 ON DELETE RESTRICT,
	CONSTRAINT users_created_by_fkey FOREIGN KEY
(
	created_by
) REFERENCES users
(
	id
)
						 ON DELETE SET NULL,
	CONSTRAINT users_updated_by_fkey FOREIGN KEY
(
	updated_by
) REFERENCES users
(
	id
)
						 ON DELETE SET NULL,
	CONSTRAINT users_deleted_by_fkey FOREIGN KEY
(
	deleted_by
) REFERENCES users
(
	id
)
						 ON DELETE SET NULL
	);

-- Deferred back-reference: roles.created_by → users
ALTER TABLE roles
	ADD CONSTRAINT roles_created_by_fkey
		FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL;

-- =============================================================
--  ROLE PERMISSIONS
-- =============================================================
CREATE TABLE IF NOT EXISTS role_permissions
(
	role_id
	bigint
	NOT
	NULL,
	permission_id
	bigint
	NOT
	NULL,
	created_at
	timestamp
	with
	time
	zone
	NOT
	NULL
	DEFAULT
	now
(
),
	created_by bigint,
	CONSTRAINT role_permissions_pkey PRIMARY KEY
(
	role_id,
	permission_id
),
	CONSTRAINT role_permissions_role_fkey FOREIGN KEY
(
	role_id
) REFERENCES roles
(
	id
) ON DELETE CASCADE,
	CONSTRAINT role_permissions_permission_fkey FOREIGN KEY
(
	permission_id
) REFERENCES permissions
(
	id
)
  ON DELETE CASCADE,
	CONSTRAINT role_permissions_created_by_fkey FOREIGN KEY
(
	created_by
) REFERENCES users
(
	id
)
  ON DELETE SET NULL
	);

-- =============================================================
--  TIER CONFIGS
-- =============================================================
CREATE TABLE IF NOT EXISTS tier_configs
(
	id
	bigint
	GENERATED
	BY
	DEFAULT AS
	IDENTITY,
	tier
	character
	varying
(
	50
) NOT NULL,
	daily_transaction_limit numeric
(
	19,
	4
) NOT NULL,
	single_transaction_limit numeric
(
	19,
	4
) NOT NULL,
	monthly_transaction_limit numeric
(
	19,
	4
) NOT NULL,
	created_at timestamp with time zone NOT NULL DEFAULT now(),
	updated_at timestamp
						 with time zone NOT NULL,
							 created_by bigint,
							 updated_by bigint,
							 CONSTRAINT tier_config_pkey PRIMARY KEY (id),
	CONSTRAINT tier_config_tier_unique UNIQUE
(
	tier
),
	CONSTRAINT tier_config_created_by_fkey FOREIGN KEY
(
	created_by
) REFERENCES users
(
	id
)
						 ON DELETE SET NULL,
	CONSTRAINT tier_config_updated_by_fkey FOREIGN KEY
(
	updated_by
) REFERENCES users
(
	id
)
						 ON DELETE SET NULL
	);

CREATE UNIQUE INDEX IF NOT EXISTS idx_tier_configs_tier
	ON tier_configs (tier);

-- =============================================================
--  MERCHANTS
-- =============================================================
CREATE TABLE IF NOT EXISTS merchants
(
	id
	bigint
	GENERATED
	BY
	DEFAULT AS
	IDENTITY,
	firstname
	character
	varying
(
	255
) NOT NULL,
	lastname character varying
(
	255
) NOT NULL,
	othername character varying
(
	255
),
	email character varying
(
	255
) NOT NULL,
	phone character varying
(
	20
) NOT NULL,
	password_hash character varying
(
	255
) NOT NULL,
	role_id bigint NOT NULL,
	address text,
	kyc_status character varying
(
	50
) NOT NULL, -- PENDING, APPROVED, REJECTED
	merchant_status character varying
(
	50
) NOT NULL, -- ACTIVE, INACTIVE, SUSPENDED, DEACTIVATED
	tier character varying
(
	50
) NOT NULL,
	created_at timestamp with time zone NOT NULL DEFAULT now(),
	updated_at timestamp with time zone NOT NULL,
	deleted_at timestamp
						 with time zone DEFAULT NULL,
							 created_by bigint,
							 updated_by bigint,
							 deleted_by bigint,
							 CONSTRAINT merchant_pkey PRIMARY KEY (id),
	CONSTRAINT merchant_email_unique UNIQUE
(
	email
),
	CONSTRAINT merchant_phone_unique UNIQUE
(
	phone
),
	CONSTRAINT merchant_role_fkey FOREIGN KEY
(
	role_id
) REFERENCES roles
(
	id
)
						 ON DELETE RESTRICT,
	CONSTRAINT merchant_created_by_fkey FOREIGN KEY
(
	created_by
) REFERENCES users
(
	id
)
						 ON DELETE SET NULL,
	CONSTRAINT merchant_updated_by_fkey FOREIGN KEY
(
	updated_by
) REFERENCES users
(
	id
)
						 ON DELETE SET NULL,
	CONSTRAINT merchant_deleted_by_fkey FOREIGN KEY
(
	deleted_by
) REFERENCES users
(
	id
)
						 ON DELETE SET NULL
	);

CREATE INDEX IF NOT EXISTS idx_merchants_tier
	ON merchants (tier);
CREATE INDEX IF NOT EXISTS idx_merchants_id_tier_covering
	ON merchants (id) INCLUDE (tier);

-- =============================================================
--  CARDS
-- =============================================================
CREATE TABLE IF NOT EXISTS cards
(
	id
	bigint
	GENERATED
	BY
	DEFAULT AS
	IDENTITY,
	merchant_id
	bigint
	NOT
	NULL,
	card_number
	character
	varying
(
	19
) NOT NULL, -- masked, e.g. 4111********1111
	card_type character varying
(
	50
) NOT NULL, -- VIRTUAL, PHYSICAL
	scheme character varying
(
	50
) NOT NULL, -- VISA, MASTERCARD, VERVE
	expiry_month smallint NOT NULL,
	expiry_year smallint NOT NULL,
	card_hash character varying
(
	64
) NOT NULL,
	card_status character varying
(
	50
) NOT NULL, -- ACTIVE, EXPIRED
	created_at timestamp with time zone NOT NULL DEFAULT now(),
	updated_at timestamp
						 with time zone NOT NULL,
							 CONSTRAINT cards_pkey PRIMARY KEY (id),
	CONSTRAINT cards_hash_unique UNIQUE
(
	card_hash
),
	CONSTRAINT cards_merchant_unique UNIQUE
(
	merchant_id
), -- one card per merchant
	CONSTRAINT cards_merchant_fkey FOREIGN KEY
(
	merchant_id
) REFERENCES merchants
(
	id
)
						 ON DELETE RESTRICT
	);

-- =============================================================
--  ACCOUNTS
--  Auto-generated by trigger on cards INSERT. Not exposed externally.
-- =============================================================
CREATE TABLE IF NOT EXISTS accounts
(
	id
	bigint
	GENERATED
	BY
	DEFAULT AS
	IDENTITY,
	card_id
	bigint
	NOT
	NULL,
	account_number
	character
	varying
(
	20
) NOT NULL,
	account_type character varying
(
	50
) NOT NULL,
	currency character varying
(
	3
) NOT NULL,
	balance numeric
(
	19,
	4
) NOT NULL DEFAULT 0,
	account_status character varying
(
	50
) NOT NULL,
	created_at timestamp with time zone NOT NULL DEFAULT now(),
	CONSTRAINT account_pkey PRIMARY KEY
(
	id
),
	CONSTRAINT account_number_unique UNIQUE
(
	account_number
),
	CONSTRAINT account_card_unique UNIQUE
(
	card_id
), -- one account per card
	CONSTRAINT account_card_fkey FOREIGN KEY
(
	card_id
) REFERENCES cards
(
	id
)
						 ON DELETE RESTRICT
	);

CREATE INDEX IF NOT EXISTS idx_accounts_account_number
	ON accounts (account_number);


-- =============================================================
--  FRAUD ATTEMPTS
-- =============================================================
CREATE TABLE IF NOT EXISTS fraud_attempts
(
	id
	bigint
	GENERATED
	BY
	DEFAULT AS
	IDENTITY,
	card_hash
	character
	varying
(
	64
) NOT NULL, -- no FK, intentional
	merchant_id bigint NOT NULL,
	ip_address character varying
(
	45
) NOT NULL,
	amount numeric
(
	19,
	4
) NOT NULL,
	currency character varying
(
	3
) NOT NULL,
	status character varying
(
	50
) NOT NULL,
	flags text[],
	created_at timestamp with time zone NOT NULL DEFAULT now(),
	CONSTRAINT fraud_attempts_pkey PRIMARY KEY
(
	id
)
	);

CREATE INDEX IF NOT EXISTS idx_fraud_attempts_card_hash
	ON fraud_attempts (card_hash);
CREATE INDEX IF NOT EXISTS idx_fraud_attempts_merchant_id
	ON fraud_attempts (merchant_id);
CREATE INDEX IF NOT EXISTS idx_fraud_attempts_ip_address
	ON fraud_attempts (ip_address);
CREATE INDEX IF NOT EXISTS idx_fraud_attempts_created_at
	ON fraud_attempts (created_at);
CREATE INDEX IF NOT EXISTS idx_fraud_attempts_status
	ON fraud_attempts (status);
CREATE INDEX IF NOT EXISTS idx_fraud_attempts_card_velocity
	ON fraud_attempts (card_hash, created_at);

-- =============================================================
--  MERCHANT BLACKLIST
-- =============================================================
CREATE TABLE IF NOT EXISTS merchant_blacklist
(
	id
	bigint
	GENERATED
	BY
	DEFAULT AS
	IDENTITY,
	merchant_id
	bigint
	NOT
	NULL,
	reason
	text
	NOT
	NULL,
	blacklisted_at
	timestamp
	with
	time
	zone
	NOT
	NULL
	DEFAULT
	now
(
),
	blacklisted_by bigint,
	lifted_at timestamp with time zone,
							lifted_by bigint,
							CONSTRAINT merchant_blacklist_pkey PRIMARY KEY (id),
	CONSTRAINT merchant_blacklist_merchant_fkey FOREIGN KEY
(
	merchant_id
) REFERENCES merchants
(
	id
)
						ON DELETE RESTRICT,
	CONSTRAINT merchant_blacklist_by_fkey FOREIGN KEY
(
	blacklisted_by
) REFERENCES users
(
	id
)
						ON DELETE SET NULL,
	CONSTRAINT merchant_blacklist_lifted_by_fkey FOREIGN KEY
(
	lifted_by
) REFERENCES users
(
	id
)
						ON DELETE SET NULL
	);

CREATE INDEX IF NOT EXISTS idx_merchant_blacklist_merchant_id
	ON merchant_blacklist (merchant_id);
CREATE INDEX IF NOT EXISTS idx_merchant_blacklist_lifted_at
	ON merchant_blacklist (lifted_at);
CREATE INDEX IF NOT EXISTS idx_merchant_blacklist_active
	ON merchant_blacklist (merchant_id) WHERE lifted_at IS NULL;