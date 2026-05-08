-- Combined fraud evaluation data query
-- Returns all fraud-related data in a single call for efficiency

CREATE
OR REPLACE FUNCTION sp_fraud_get_evaluation_data(
    p_account_number TEXT,
    p_card_hash      TEXT,
    p_since          TIMESTAMPTZ
)
RETURNS TABLE (
    is_blacklisted    BOOLEAN,
    velocity_count    INT,
    transaction_limit NUMERIC,
    merchant_id       BIGINT
)
LANGUAGE plpgsql
AS $$
BEGIN
RETURN QUERY
SELECT COALESCE((SELECT EXISTS (SELECT 1
								FROM merchant_blacklist mb
								WHERE mb.merchant_id = a.merchant_id
								  AND mb.lifted_at IS NULL)), false) AS is_blacklisted,
	   (SELECT COUNT(*) ::INT
		FROM fraud_attempts fa
		WHERE fa.card_hash = p_card_hash
		  AND fa.created_at >= p_since)                              AS velocity_count,
	   tc.single_transaction_limit                                   AS transaction_limit,
	   a.merchant_id
FROM accounts a
		 JOIN merchants m ON m.id = a.merchant_id
		 JOIN tier_configs tc ON tc.tier = m.tier
WHERE a.account_number = p_account_number LIMIT 1;
END;
$$;