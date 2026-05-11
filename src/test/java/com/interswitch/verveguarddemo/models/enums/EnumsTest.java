package com.interswitch.verveguarddemo.models.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EnumsTest {

    @Test
    void accountType_hasExpectedValues() {
        assertThat(AccountType.values()).contains(
                AccountType.SETTLEMENT,
                AccountType.ESCROW,
                AccountType.WALLET,
                AccountType.VIRTUAL
        );
    }

    @Test
    void accountStatus_hasExpectedValues() {
        assertThat(AccountStatus.values()).contains(
                AccountStatus.ACTIVE,
                AccountStatus.SUSPENDED,
                AccountStatus.CLOSED,
                AccountStatus.FROZEN
        );
    }

    @Test
    void cardType_hasExpectedValues() {
        assertThat(CardType.values()).contains(
                CardType.VIRTUAL,
                CardType.PHYSICAL
        );
    }

    @Test
    void cardScheme_hasExpectedValues() {
        assertThat(CardScheme.values()).contains(
                CardScheme.VISA,
                CardScheme.MASTERCARD,
                CardScheme.VERVE
        );
    }

    @Test
    void cardStatus_hasExpectedValues() {
        assertThat(CardStatus.values()).contains(
                CardStatus.ACTIVE,
                CardStatus.BLOCKED,
                CardStatus.EXPIRED
        );
    }

    @Test
    void fraudStatus_hasExpectedValues() {
        assertThat(FraudStatus.values()).contains(
                FraudStatus.CLEAN,
                FraudStatus.SUSPICIOUS,
                FraudStatus.BLOCKED
        );
    }

    @Test
    void kycStatus_hasExpectedValues() {
        assertThat(KycStatus.values()).contains(
                KycStatus.PENDING,
                KycStatus.IN_REVIEW,
                KycStatus.APPROVED,
                KycStatus.REJECTED,
                KycStatus.SUSPENDED
        );
    }

    @Test
    void merchantStatus_hasExpectedValues() {
        assertThat(MerchantStatus.values()).contains(
                MerchantStatus.ACTIVE,
                MerchantStatus.INACTIVE,
                MerchantStatus.SUSPENDED
        );
    }

    @Test
    void merchantTier_hasExpectedValues() {
        assertThat(MerchantTier.values()).contains(
                MerchantTier.TIER_1,
                MerchantTier.TIER_2,
                MerchantTier.TIER_3
        );
    }

    @Test
    void principalType_hasExpectedValues() {
        assertThat(PrincipalType.values()).contains(
                PrincipalType.MERCHANT,
                PrincipalType.ADMIN
        );
    }

    @Test
    void userStatus_hasExpectedValues() {
        assertThat(UserStatus.values()).contains(
                UserStatus.ACTIVE,
                UserStatus.INACTIVE,
                UserStatus.SUSPENDED,
                UserStatus.DEACTIVATED
        );
    }

    @Test
    void transactionType_hasExpectedValues() {
        assertThat(TransactionType.values()).contains(
                TransactionType.DEBIT,
                TransactionType.CREDIT
        );
    }

    @Test
    void transactionStatus_hasExpectedValues() {
        assertThat(TransactionStatus.values()).contains(
                TransactionStatus.PENDING,
                TransactionStatus.SUCCESS,
                TransactionStatus.FAILED,
                TransactionStatus.REVERSED
        );
    }

    @Test
    void transactionChannel_hasExpectedValues() {
        assertThat(TransactionChannel.values()).contains(
                TransactionChannel.CARD,
                TransactionChannel.TRANSFER,
                TransactionChannel.USSD
        );
    }

    @Test
    void transferStatus_hasExpectedValues() {
        assertThat(TransferStatus.values()).contains(
                TransferStatus.PENDING,
                TransferStatus.SUCCESS,
                TransferStatus.FAILED,
                TransferStatus.REVERSED
        );
    }

    @Test
    void emailStatus_hasExpectedValues() {
        assertThat(EmailStatus.values()).contains(
                EmailStatus.QUEUED,
                EmailStatus.SENDING,
                EmailStatus.SENT,
                EmailStatus.FAILED
        );
    }

    @Test
    void enumValueOf_works() {
        assertThat(PrincipalType.valueOf("MERCHANT")).isEqualTo(PrincipalType.MERCHANT);
        assertThat(PrincipalType.valueOf("ADMIN")).isEqualTo(PrincipalType.ADMIN);
    }

    @Test
    void enumName_works() {
        assertThat(PrincipalType.MERCHANT.name()).isEqualTo("MERCHANT");
        assertThat(MerchantTier.TIER_1.name()).isEqualTo("TIER_1");
    }
}
