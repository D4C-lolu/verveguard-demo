
CREATE INDEX IF NOT EXISTS idx_accounts_account_number
    ON accounts (account_number);

CREATE INDEX IF NOT EXISTS idx_fraud_attempts_card_velocity
    ON fraud_attempts (card_hash, created_at);


CREATE INDEX IF NOT EXISTS idx_merchant_blacklist_active
    ON merchant_blacklist (merchant_id)
    WHERE lifted_at IS NULL;


CREATE INDEX IF NOT EXISTS idx_merchants_tier
    ON merchants (tier);

CREATE UNIQUE INDEX IF NOT EXISTS idx_tier_configs_tier
    ON tier_configs (tier);

CREATE INDEX IF NOT EXISTS idx_merchants_id_tier_covering
    ON merchants (id)
    INCLUDE (tier);