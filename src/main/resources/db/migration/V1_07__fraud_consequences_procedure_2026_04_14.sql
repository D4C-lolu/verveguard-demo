CREATE OR REPLACE FUNCTION blacklist_merchant(
    p_merchant_id    bigint,
    p_reason         text,
    p_blacklisted_by bigint
) RETURNS void
LANGUAGE plpgsql AS $$
BEGIN
    -- Suspend the merchant
UPDATE merchants
SET merchant_status = 'SUSPENDED',
    updated_at      = now(),
    updated_by      = p_blacklisted_by
WHERE id = p_merchant_id;

-- Block the merchant's card
UPDATE cards
SET card_status = 'BLOCKED',
    updated_at  = now()
WHERE merchant_id = p_merchant_id;

-- Insert blacklist record
INSERT INTO merchant_blacklist (merchant_id, reason, blacklisted_by)
VALUES (p_merchant_id, p_reason, p_blacklisted_by);
END;
$$;

CREATE OR REPLACE FUNCTION sp_card_block_by_hash(
    p_card_hash  character varying,
    p_updated_by bigint
) RETURNS boolean
LANGUAGE plpgsql AS $$
DECLARE
v_rows_affected int;
BEGIN
UPDATE cards
SET card_status = 'BLOCKED',
    updated_at  = now()
WHERE card_hash = p_card_hash
  AND card_status != 'BLOCKED';

GET DIAGNOSTICS v_rows_affected = ROW_COUNT;
RETURN v_rows_affected > 0;
END;
$$;

CREATE OR REPLACE FUNCTION get_merchant_alert_info(p_card_hash character varying)
RETURNS TABLE (email character varying, fullname text)
LANGUAGE plpgsql AS $$
BEGIN
RETURN QUERY
SELECT m.email,
       m.firstname || ' ' || m.lastname
FROM merchants m
         JOIN cards c ON c.merchant_id = m.id
WHERE c.card_hash = p_card_hash
  AND m.deleted_at IS NULL
    LIMIT 1;
END;
$$;