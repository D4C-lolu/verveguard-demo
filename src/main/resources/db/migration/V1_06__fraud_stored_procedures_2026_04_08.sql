-- Single combined evaluation call — blacklist + transaction limit in one query
CREATE OR REPLACE FUNCTION sp_fraud_get_evaluation_data(
    p_card_hash TEXT
)
RETURNS TABLE (
    is_card_blocked         BOOLEAN,
    is_merchant_blacklisted BOOLEAN,
    transaction_limit       NUMERIC,
    merchant_id             BIGINT
)
LANGUAGE plpgsql AS $$
BEGIN
RETURN QUERY
SELECT
	(c.card_status = 'BLOCKED')  AS is_card_blocked,
	(mb.merchant_id IS NOT NULL) AS is_merchant_blacklisted,
	tc.single_transaction_limit  AS transaction_limit,
	c.merchant_id
FROM  cards c
		  JOIN  merchants m      ON m.id    = c.merchant_id
		  JOIN  tier_configs tc  ON tc.tier = m.tier
		  LEFT JOIN merchant_blacklist mb
					ON mb.merchant_id = c.merchant_id
						AND mb.lifted_at   IS NULL
WHERE c.card_hash = p_card_hash
	LIMIT 1;
END;
$$;

-- Insert fraud attempt
CREATE
OR REPLACE FUNCTION sp_fraud_insert_attempt(
    p_card_hash   text,
    p_merchant_id bigint,
    p_ip_address  text,
    p_amount      numeric,
    p_currency    text,
    p_status      text,
    p_flags       text[]
) RETURNS void LANGUAGE plpgsql AS $$
BEGIN
INSERT INTO fraud_attempts (card_hash, merchant_id, ip_address, amount, currency, status, flags)
VALUES (p_card_hash, p_merchant_id, p_ip_address, p_amount, p_currency, p_status, p_flags);
END;
$$;

-- Velocity count (card_hash direct, for when snapshot is unavailable)
CREATE
OR REPLACE FUNCTION sp_fraud_get_card_velocity_count(
    p_card_hash text,
    p_since     timestamptz
) RETURNS int LANGUAGE plpgsql AS $$
DECLARE
v_count int;
BEGIN
SELECT COUNT(*) ::int
INTO v_count
FROM fraud_attempts
WHERE card_hash = p_card_hash
  AND created_at >= p_since;
RETURN v_count;
END;
$$;

-- Blacklist check fallback by card number
CREATE
OR REPLACE FUNCTION sp_fraud_is_blacklisted_by_card_number(p_card_number text)
RETURNS boolean LANGUAGE plpgsql AS $$
BEGIN
RETURN EXISTS (SELECT 1
			   FROM cards c
						JOIN merchant_blacklist mb ON mb.merchant_id = c.merchant_id
			   WHERE c.card_hash = encode(digest(p_card_number, 'sha256'), 'hex')
				 AND mb.lifted_at IS NULL);
END;
$$;

-- Transaction limit fallback by card number
CREATE
OR REPLACE FUNCTION sp_fraud_get_transaction_limit_by_card_number(p_card_number text)
RETURNS numeric LANGUAGE plpgsql AS $$
DECLARE
v_limit numeric;
BEGIN
SELECT tc.single_transaction_limit
INTO v_limit
FROM cards c
		 JOIN merchants m ON m.id = c.merchant_id
		 JOIN tier_configs tc ON tc.tier = m.tier
WHERE c.card_hash = encode(digest(p_card_number, 'sha256'), 'hex');
RETURN v_limit;
END;
$$;

CREATE
OR REPLACE FUNCTION sp_fraud_get_attempts(
    p_limit  int,
    p_offset bigint
) RETURNS TABLE (
    id              bigint,
    card_hash       text,
    merchant_id     bigint,
    merchant_firstname text,
    merchant_lastname  text,
    merchant_othername text,
    merchant_email     text,
    merchant_phone     text,
    ip_address      text,
    amount          numeric,
    currency        text,
    status          text,
    flags           text[],
    created_at      timestamptz,
    total_count     bigint
) LANGUAGE plpgsql AS $$
BEGIN
RETURN QUERY
SELECT fa.id,
	   fa.card_hash::text, fa.merchant_id,
	   m.firstname::text, m.lastname::text, m.othername::text, m.email::text, m.phone::text, fa.ip_address::text, fa.amount,
	   fa.currency::text, fa.status::text, fa.flags,
	   fa.created_at,
	   COUNT(*) OVER () AS total_count
FROM fraud_attempts fa
		 JOIN merchants m ON m.id = fa.merchant_id
ORDER BY fa.created_at DESC LIMIT p_limit
OFFSET p_offset;
END;
$$;