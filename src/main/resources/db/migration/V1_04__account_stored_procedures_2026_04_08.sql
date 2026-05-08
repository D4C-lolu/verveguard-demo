-- Merchant: find their own account via merchant_id
CREATE
OR REPLACE FUNCTION sp_account_find_by_merchant(p_merchant_id bigint)
RETURNS TABLE (
    id             bigint,
    merchant_id    bigint,
    account_number character varying,
    account_type   character varying,
    currency       character varying,
    balance        numeric,
    created_at     timestamp with time zone
) LANGUAGE plpgsql AS $$
BEGIN
RETURN QUERY
SELECT a.id,
	   c.merchant_id,
	   a.account_number,
	   a.account_type,
	   a.currency,
	   a.balance,
	   a.created_at
FROM accounts a
		 JOIN cards c ON c.id = a.card_id
WHERE c.merchant_id = p_merchant_id;
END;
$$;

-- Admin: find account by card_number via pgcrypto hash
CREATE
OR REPLACE FUNCTION sp_account_find_by_card_number(p_card_number text)
RETURNS TABLE (
    id             bigint,
    merchant_id    bigint,
    account_number character varying,
    account_type   character varying,
    currency       character varying,
    balance        numeric,
    created_at     timestamp with time zone
) LANGUAGE plpgsql AS $$
BEGIN
RETURN QUERY
SELECT a.id,
	   c.merchant_id,
	   a.account_number,
	   a.account_type,
	   a.currency,
	   a.balance,
	   a.created_at
FROM accounts a
		 JOIN cards c ON c.id = a.card_id
WHERE c.card_hash = encode(digest(p_card_number, 'sha256'), 'hex');
END;
$$;

-- Admin: find all accounts, paginated
CREATE
OR REPLACE FUNCTION sp_account_find_all(
    p_limit      int,
    p_offset     bigint,
    p_sort_field character varying,
    p_sort_dir   character varying
)
RETURNS TABLE (
    id             bigint,
    merchant_id    bigint,
    account_number character varying,
    account_type   character varying,
    currency       character varying,
    balance        numeric,
    created_at     timestamp with time zone,
    total_count    bigint
) LANGUAGE plpgsql AS $$
BEGIN
RETURN QUERY EXECUTE format(
        'SELECT a.id, c.merchant_id, a.account_number, a.account_type,
                a.currency, a.balance, a.created_at,
                COUNT(*) OVER () AS total_count
         FROM accounts a
         JOIN cards c ON c.id = a.card_id
         ORDER BY %I %s
         LIMIT $1 OFFSET $2',
        p_sort_field, p_sort_dir
    ) USING p_limit, p_offset;
END;
$$;

