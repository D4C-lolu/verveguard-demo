DO $$
	DECLARE
		i                INT;
		merchant_role_id BIGINT;
		new_merchant_id  BIGINT;
		new_card_id      BIGINT;
		raw_pan          TEXT;
		masked_pan       TEXT;
		pan_hash         TEXT;
	BEGIN
		SELECT id
		INTO merchant_role_id
		FROM roles
		WHERE name = 'MERCHANT';
	
		FOR i IN 1..200 LOOP
				raw_pan    := '4' || LPAD(i::TEXT, 15, '0');
				masked_pan
		:= LEFT(raw_pan, 4) || '********' || RIGHT(raw_pan, 4);
				pan_hash
		:= encode(digest(raw_pan, 'sha256'), 'hex');
	
		INSERT INTO merchants (firstname, lastname, email, phone,
							   password_hash, role_id,
							   address, kyc_status, merchant_status, tier,
							   created_at, updated_at)
		VALUES ('Stress',
				'Merchant' || i,
				'merchant' || i || '@stresstest.com',
				'5' || LPAD(i::TEXT, 10, '0'),
				'$2a$10$mRxdXj7YESYMm/DIxn9X9OVXykdCww6ZEUX9a1jrOY2GildUkm4Ji',
				merchant_role_id,
				i || ' Stress Test Street, Lagos',
				'APPROVED', 'ACTIVE', 'TIER_3',
				NOW(), NOW()) RETURNING id
		INTO new_merchant_id;
		
		INSERT INTO cards (merchant_id, card_number, card_hash,
						   card_type, scheme,
						   expiry_month, expiry_year, card_status,
						   created_at, updated_at)
		VALUES (new_merchant_id,
				masked_pan,
				pan_hash,
				'VIRTUAL', 'VISA',
				12, 2028, 'ACTIVE',
				NOW(), NOW()) RETURNING id
		INTO new_card_id;
		
		INSERT INTO accounts (card_id, account_number, account_type,
							  currency, balance, account_status,
							  created_at)
		VALUES (new_card_id,
				'33' || LPAD(i::TEXT, 8, '0'),
				'SETTLEMENT', 'NGN',
				100000000.0000, 'ACTIVE',
				NOW());

	END LOOP;
END $$;