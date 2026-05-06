
-- Insert card, returns new card id
CREATE OR REPLACE FUNCTION sp_card_insert(
    p_merchant_id  bigint,
    p_card_number  character varying,
    p_card_hash    character varying,
    p_card_type    character varying,
    p_scheme       character varying,
    p_expiry_month smallint,
    p_expiry_year  smallint,
    p_card_status  character varying,
    p_created_by   bigint
) RETURNS bigint LANGUAGE plpgsql AS $$
DECLARE
v_card_id bigint;
BEGIN
INSERT INTO cards (merchant_id, card_number, card_hash, card_type, scheme,
                   expiry_month, expiry_year, card_status, created_at, updated_at, created_by)
VALUES (p_merchant_id, p_card_number, p_card_hash, p_card_type, p_scheme,
        p_expiry_month, p_expiry_year, p_card_status, now(), now(), p_created_by)
    RETURNING id INTO v_card_id;
RETURN v_card_id;
END;
$$;

-- Generate account for card
CREATE OR REPLACE FUNCTION sp_account_create_for_card(p_card_id bigint)
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
INSERT INTO accounts (card_id, account_number, account_type, currency, balance)
VALUES (
           p_card_id,
           LPAD(nextval('account_number_seq')::text, 10, '0'),
           'MERCHANT',
           'NGN',
           ROUND((random() * 4990000 + 10000)::numeric, 4)
       );
END;
$$;

-- Validate card creation (no existing card for merchant, card hash not duplicate)
CREATE OR REPLACE FUNCTION sp_card_get_creation_validation(
    p_merchant_id bigint,
    p_card_hash   character varying
) RETURNS TABLE (
    kyc_status      character varying,
    card_hash_exists boolean,
    already_has_card boolean
) LANGUAGE plpgsql AS $$
BEGIN
RETURN QUERY
SELECT
    m.kyc_status,
    EXISTS (SELECT 1 FROM cards WHERE card_hash = p_card_hash)          AS card_hash_exists,
    EXISTS (SELECT 1 FROM cards WHERE merchant_id = p_merchant_id)      AS already_has_card
FROM merchants m
WHERE m.id = p_merchant_id;
END;
$$;

-- Find card by merchant_id (merchant self-serve), includes account info
CREATE OR REPLACE FUNCTION sp_card_find_by_merchant(p_merchant_id bigint)
RETURNS TABLE (
    id             bigint,
    merchant_id    bigint,
    card_number    character varying,
    card_type      character varying,
    scheme         character varying,
    expiry_month   smallint,
    expiry_year    smallint,
    card_status    character varying,
    account_number character varying,
    account_type   character varying,
    currency       character varying,
    balance        numeric,
    created_at     timestamp with time zone,
    updated_at     timestamp with time zone
) LANGUAGE plpgsql AS $$
BEGIN
RETURN QUERY
SELECT c.id, c.merchant_id, c.card_number, c.card_type, c.scheme,
       c.expiry_month, c.expiry_year, c.card_status,
       a.account_number, a.account_type, a.currency, a.balance,
       c.created_at, c.updated_at
FROM cards c
         JOIN accounts a ON a.card_id = c.id
WHERE c.merchant_id = p_merchant_id;
END;
$$;

-- Admin: find card by card number (hashed via pgcrypto)
CREATE OR REPLACE FUNCTION sp_card_find_by_card_number(p_card_number text)
RETURNS TABLE (
    id             bigint,
    merchant_id    bigint,
    card_number    character varying,
    card_type      character varying,
    scheme         character varying,
    expiry_month   smallint,
    expiry_year    smallint,
    card_status    character varying,
    account_number character varying,
    account_type   character varying,
    currency       character varying,
    balance        numeric,
    created_at     timestamp with time zone,
    updated_at     timestamp with time zone
) LANGUAGE plpgsql AS $$
BEGIN
RETURN QUERY
SELECT c.id, c.merchant_id, c.card_number, c.card_type, c.scheme,
       c.expiry_month, c.expiry_year, c.card_status,
       a.account_number, a.account_type, a.currency, a.balance,
       c.created_at, c.updated_at
FROM cards c
         JOIN accounts a ON a.card_id = c.id
WHERE c.card_hash = encode(digest(p_card_number, 'sha256'), 'hex');
END;
$$;

-- Admin: find all cards, paginated, with account fields
CREATE OR REPLACE FUNCTION sp_card_find_all(
    p_limit      int,
    p_offset     bigint,
    p_sort_field character varying,
    p_sort_dir   character varying
) RETURNS TABLE (
    id             bigint,
    merchant_id    bigint,
    card_number    character varying,
    card_type      character varying,
    scheme         character varying,
    expiry_month   smallint,
    expiry_year    smallint,
    card_status    character varying,
    account_number character varying,
    account_type   character varying,
    currency       character varying,
    balance        numeric,
    created_at     timestamp with time zone,
    updated_at     timestamp with time zone,
    total_count    bigint
) LANGUAGE plpgsql AS $$
BEGIN
RETURN QUERY EXECUTE format(
        'SELECT c.id, c.merchant_id, c.card_number, c.card_type, c.scheme,
                c.expiry_month, c.expiry_year, c.card_status,
                a.account_number, a.account_type, a.currency, a.balance,
                c.created_at, c.updated_at,
                COUNT(*) OVER () AS total_count
         FROM cards c
         JOIN accounts a ON a.card_id = c.id
         ORDER BY %I %s
         LIMIT $1 OFFSET $2',
        p_sort_field, p_sort_dir
    ) USING p_limit, p_offset;
END;
$$;

-- Block card manually
CREATE OR REPLACE FUNCTION sp_card_block(p_id bigint, p_updated_by bigint)
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
UPDATE cards SET card_status = 'BLOCKED', updated_at = now(), updated_by = p_updated_by
WHERE id = p_id;
END;
$$;

-- Expire cards (cron job)
CREATE OR REPLACE FUNCTION sp_card_expire_due()
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
UPDATE cards
SET card_status = 'EXPIRED', updated_at = now()
WHERE card_status = 'ACTIVE'
  AND (expiry_year, expiry_month) < (EXTRACT(YEAR FROM now()), EXTRACT(MONTH FROM now()));
END;
$$;

-- Check card exists
CREATE OR REPLACE FUNCTION sp_card_exists(p_id bigint)
RETURNS boolean LANGUAGE plpgsql AS $$
BEGIN
RETURN EXISTS (SELECT 1 FROM cards WHERE id = p_id);
END;
$$;