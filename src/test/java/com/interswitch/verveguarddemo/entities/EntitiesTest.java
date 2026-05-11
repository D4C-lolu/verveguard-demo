package com.interswitch.verveguarddemo.entities;

import com.interswitch.verveguarddemo.models.enums.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EntitiesTest {

    @Nested
    class UserEntityTest {

        @Test
        void idOnlyConstructor() {
            User user = new User(1L);
            assertThat(user.getId()).isEqualTo(1L);
        }

        @Test
        void builderAndGetters() {
            Role role = Role.builder().id(1L).name("ADMIN").build();
            User user = User.builder()
                    .id(1L)
                    .firstname("John")
                    .lastname("Doe")
                    .othername("Middle")
                    .email("john@example.com")
                    .phone("1234567890")
                    .passwordHash("hash")
                    .role(role)
                    .userStatus(UserStatus.ACTIVE)
                    .build();

            assertThat(user.getFirstname()).isEqualTo("John");
            assertThat(user.getLastname()).isEqualTo("Doe");
            assertThat(user.getOthername()).isEqualTo("Middle");
            assertThat(user.getEmail()).isEqualTo("john@example.com");
            assertThat(user.getPhone()).isEqualTo("1234567890");
            assertThat(user.getPasswordHash()).isEqualTo("hash");
            assertThat(user.getRole()).isEqualTo(role);
            assertThat(user.getUserStatus()).isEqualTo(UserStatus.ACTIVE);
        }

        @Test
        void equalsAndHashCode_sameId() {
            User user1 = new User(1L);
            User user2 = new User(1L);

            assertThat(user1).isEqualTo(user2);
            assertThat(user1.hashCode()).isEqualTo(user2.hashCode());
        }

        @Test
        void equalsAndHashCode_differentId() {
            User user1 = new User(1L);
            User user2 = new User(2L);

            assertThat(user1).isNotEqualTo(user2);
        }

        @Test
        void equalsAndHashCode_nullId() {
            User user1 = new User();
            User user2 = new User();

            assertThat(user1).isNotEqualTo(user2);
        }

        @Test
        void equals_withNull() {
            User user = new User(1L);
            assertThat(user).isNotEqualTo(null);
        }

        @Test
        void equals_withSelf() {
            User user = new User(1L);
            assertThat(user).isEqualTo(user);
        }

        @Test
        void equals_withDifferentClass() {
            User user = new User(1L);
            assertThat(user).isNotEqualTo("not a user");
        }

        @Test
        void toStringContainsIdAndEmail() {
            User user = User.builder().id(1L).email("test@example.com").build();
            String str = user.toString();

            assertThat(str).contains("1");
            assertThat(str).contains("test@example.com");
        }
    }

    @Nested
    class MerchantEntityTest {

        @Test
        void idOnlyConstructor() {
            Merchant merchant = new Merchant(1L);
            assertThat(merchant.getId()).isEqualTo(1L);
        }

        @Test
        void builderAndGetters() {
            Role role = Role.builder().id(1L).name("MERCHANT").build();
            Merchant merchant = Merchant.builder()
                    .id(1L)
                    .firstname("Jane")
                    .lastname("Smith")
                    .othername("M")
                    .email("jane@example.com")
                    .phone("0987654321")
                    .passwordHash("merchantHash")
                    .role(role)
                    .address("123 Main St")
                    .kycStatus(KycStatus.APPROVED)
                    .merchantStatus(MerchantStatus.ACTIVE)
                    .tier(MerchantTier.TIER_1)
                    .build();

            assertThat(merchant.getFirstname()).isEqualTo("Jane");
            assertThat(merchant.getLastname()).isEqualTo("Smith");
            assertThat(merchant.getAddress()).isEqualTo("123 Main St");
            assertThat(merchant.getKycStatus()).isEqualTo(KycStatus.APPROVED);
            assertThat(merchant.getMerchantStatus()).isEqualTo(MerchantStatus.ACTIVE);
            assertThat(merchant.getTier()).isEqualTo(MerchantTier.TIER_1);
        }

        @Test
        void equalsAndHashCode_sameId() {
            Merchant m1 = new Merchant(1L);
            Merchant m2 = new Merchant(1L);

            assertThat(m1).isEqualTo(m2);
            assertThat(m1.hashCode()).isEqualTo(m2.hashCode());
        }

        @Test
        void equalsAndHashCode_differentId() {
            Merchant m1 = new Merchant(1L);
            Merchant m2 = new Merchant(2L);

            assertThat(m1).isNotEqualTo(m2);
        }

        @Test
        void equalsAndHashCode_nullId() {
            Merchant m1 = new Merchant();
            Merchant m2 = new Merchant();

            assertThat(m1).isNotEqualTo(m2);
        }

        @Test
        void equals_withNull() {
            Merchant merchant = new Merchant(1L);
            assertThat(merchant).isNotEqualTo(null);
        }

        @Test
        void toStringContainsRelevantFields() {
            Merchant merchant = Merchant.builder()
                    .id(1L)
                    .email("m@example.com")
                    .kycStatus(KycStatus.PENDING)
                    .merchantStatus(MerchantStatus.INACTIVE)
                    .tier(MerchantTier.TIER_2)
                    .build();

            String str = merchant.toString();
            assertThat(str).contains("1");
            assertThat(str).contains("m@example.com");
        }
    }

    @Nested
    class CardEntityTest {

        @Test
        void idOnlyConstructor() {
            Card card = new Card(1L);
            assertThat(card.getId()).isEqualTo(1L);
        }

        @Test
        void builderAndGetters() {
            Merchant merchant = new Merchant(1L);
            Card card = Card.builder()
                    .id(1L)
                    .cardNumber("4111111111111111")
                    .cardHash("hashedValue")
                    .merchant(merchant)
                    .cardType(CardType.VIRTUAL)
                    .scheme(CardScheme.VISA)
                    .expiryMonth((short) 12)
                    .expiryYear((short) 2025)
                    .cardStatus(CardStatus.ACTIVE)
                    .build();

            assertThat(card.getCardNumber()).isEqualTo("4111111111111111");
            assertThat(card.getCardHash()).isEqualTo("hashedValue");
            assertThat(card.getMerchant()).isEqualTo(merchant);
            assertThat(card.getCardType()).isEqualTo(CardType.VIRTUAL);
            assertThat(card.getScheme()).isEqualTo(CardScheme.VISA);
            assertThat(card.getExpiryMonth()).isEqualTo((short) 12);
            assertThat(card.getExpiryYear()).isEqualTo((short) 2025);
            assertThat(card.getCardStatus()).isEqualTo(CardStatus.ACTIVE);
        }

        @Test
        void equalsAndHashCode_sameId() {
            Card c1 = new Card(1L);
            Card c2 = new Card(1L);

            assertThat(c1).isEqualTo(c2);
            assertThat(c1.hashCode()).isEqualTo(c2.hashCode());
        }

        @Test
        void equalsAndHashCode_differentId() {
            Card c1 = new Card(1L);
            Card c2 = new Card(2L);

            assertThat(c1).isNotEqualTo(c2);
        }

        @Test
        void equalsAndHashCode_nullId() {
            Card c1 = new Card();
            Card c2 = new Card();

            assertThat(c1).isNotEqualTo(c2);
        }

        @Test
        void equals_withNull() {
            Card card = new Card(1L);
            assertThat(card).isNotEqualTo(null);
        }

        @Test
        void toStringContainsRelevantFields() {
            Card card = Card.builder()
                    .id(1L)
                    .cardNumber("4111")
                    .cardType(CardType.PHYSICAL)
                    .scheme(CardScheme.MASTERCARD)
                    .cardStatus(CardStatus.BLOCKED)
                    .build();

            String str = card.toString();
            assertThat(str).contains("1");
            assertThat(str).contains("4111");
        }
    }

    @Nested
    class AccountEntityTest {

        @Test
        void idOnlyConstructor() {
            Account account = new Account(1L);
            assertThat(account.getId()).isEqualTo(1L);
        }

        @Test
        void builderAndGetters() {
            Card card = new Card(1L);
            OffsetDateTime now = OffsetDateTime.now();
            Account account = Account.builder()
                    .id(1L)
                    .card(card)
                    .accountNumber("1234567890")
                    .accountType(AccountType.SETTLEMENT)
                    .currency("NGN")
                    .balance(BigDecimal.valueOf(1000.50))
                    .createdAt(now)
                    .build();

            assertThat(account.getCard()).isEqualTo(card);
            assertThat(account.getAccountNumber()).isEqualTo("1234567890");
            assertThat(account.getAccountType()).isEqualTo(AccountType.SETTLEMENT);
            assertThat(account.getCurrency()).isEqualTo("NGN");
            assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(1000.50));
            assertThat(account.getCreatedAt()).isEqualTo(now);
        }

        @Test
        void equalsAndHashCode_sameId() {
            Account a1 = new Account(1L);
            Account a2 = new Account(1L);

            assertThat(a1).isEqualTo(a2);
            assertThat(a1.hashCode()).isEqualTo(a2.hashCode());
        }

        @Test
        void equalsAndHashCode_differentId() {
            Account a1 = new Account(1L);
            Account a2 = new Account(2L);

            assertThat(a1).isNotEqualTo(a2);
        }

        @Test
        void equalsAndHashCode_nullId() {
            Account a1 = new Account();
            Account a2 = new Account();

            assertThat(a1).isNotEqualTo(a2);
        }

        @Test
        void equals_withNull() {
            Account account = new Account(1L);
            assertThat(account).isNotEqualTo(null);
        }

        @Test
        void toStringContainsRelevantFields() {
            Account account = Account.builder()
                    .id(1L)
                    .accountNumber("ACC123")
                    .currency("USD")
                    .balance(BigDecimal.valueOf(500))
                    .build();

            String str = account.toString();
            assertThat(str).contains("1");
            assertThat(str).contains("ACC123");
            assertThat(str).contains("USD");
        }
    }

    @Nested
    class RoleEntityTest {

        @Test
        void idOnlyConstructor() {
            Role role = new Role(1L);
            assertThat(role.getId()).isEqualTo(1L);
        }

        @Test
        void builderAndGetters() {
            Permission permission = Permission.builder().id(1L).name("READ").build();
            Role role = Role.builder()
                    .id(1L)
                    .name("ADMIN")
                    .principalType(PrincipalType.ADMIN)
                    .permissions(List.of(permission))
                    .build();

            assertThat(role.getName()).isEqualTo("ADMIN");
            assertThat(role.getPrincipalType()).isEqualTo(PrincipalType.ADMIN);
            assertThat(role.getPermissions()).containsExactly(permission);
        }

        @Test
        void equalsAndHashCode_sameId() {
            Role r1 = new Role(1L);
            Role r2 = new Role(1L);

            assertThat(r1).isEqualTo(r2);
            assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
        }

        @Test
        void equalsAndHashCode_differentId() {
            Role r1 = new Role(1L);
            Role r2 = new Role(2L);

            assertThat(r1).isNotEqualTo(r2);
        }

        @Test
        void equalsAndHashCode_nullId() {
            Role r1 = new Role();
            Role r2 = new Role();

            assertThat(r1).isNotEqualTo(r2);
        }

        @Test
        void equals_withNull() {
            Role role = new Role(1L);
            assertThat(role).isNotEqualTo(null);
        }

        @Test
        void toStringContainsRelevantFields() {
            Role role = Role.builder().id(1L).name("MERCHANT").build();

            String str = role.toString();
            assertThat(str).contains("1");
            assertThat(str).contains("MERCHANT");
        }
    }

    @Nested
    class MerchantBlacklistEntityTest {

        @Test
        void builderAndGetters() {
            Merchant merchant = new Merchant(1L);
            User blacklistedBy = new User(2L);
            User liftedBy = new User(3L);
            OffsetDateTime blacklistedAt = OffsetDateTime.now();
            OffsetDateTime liftedAt = blacklistedAt.plusDays(1);

            MerchantBlacklist blacklist = MerchantBlacklist.builder()
                    .id(1L)
                    .merchant(merchant)
                    .reason("Fraudulent activity")
                    .blacklistedAt(blacklistedAt)
                    .blacklistedBy(blacklistedBy)
                    .liftedAt(liftedAt)
                    .liftedBy(liftedBy)
                    .build();

            assertThat(blacklist.getId()).isEqualTo(1L);
            assertThat(blacklist.getMerchant()).isEqualTo(merchant);
            assertThat(blacklist.getReason()).isEqualTo("Fraudulent activity");
            assertThat(blacklist.getBlacklistedAt()).isEqualTo(blacklistedAt);
            assertThat(blacklist.getBlacklistedBy()).isEqualTo(blacklistedBy);
            assertThat(blacklist.getLiftedAt()).isEqualTo(liftedAt);
            assertThat(blacklist.getLiftedBy()).isEqualTo(liftedBy);
        }

        @Test
        void equalsAndHashCode_sameId() {
            MerchantBlacklist b1 = MerchantBlacklist.builder().id(1L).build();
            MerchantBlacklist b2 = MerchantBlacklist.builder().id(1L).build();

            assertThat(b1).isEqualTo(b2);
            assertThat(b1.hashCode()).isEqualTo(b2.hashCode());
        }

        @Test
        void equalsAndHashCode_differentId() {
            MerchantBlacklist b1 = MerchantBlacklist.builder().id(1L).build();
            MerchantBlacklist b2 = MerchantBlacklist.builder().id(2L).build();

            assertThat(b1).isNotEqualTo(b2);
        }

        @Test
        void equalsAndHashCode_nullId() {
            MerchantBlacklist b1 = new MerchantBlacklist();
            MerchantBlacklist b2 = new MerchantBlacklist();

            assertThat(b1).isNotEqualTo(b2);
        }

        @Test
        void equals_withNull() {
            MerchantBlacklist blacklist = MerchantBlacklist.builder().id(1L).build();
            assertThat(blacklist).isNotEqualTo(null);
        }

        @Test
        void toStringContainsRelevantFields() {
            OffsetDateTime blacklistedAt = OffsetDateTime.now();
            MerchantBlacklist blacklist = MerchantBlacklist.builder()
                    .id(1L)
                    .blacklistedAt(blacklistedAt)
                    .build();

            String str = blacklist.toString();
            assertThat(str).contains("1");
        }
    }

    @Nested
    class PermissionEntityTest {

        @Test
        void idOnlyConstructor() {
            Permission permission = new Permission(1L);
            assertThat(permission.getId()).isEqualTo(1L);
        }

        @Test
        void builderAndGetters() {
            Permission permission = Permission.builder()
                    .id(1L)
                    .name("READ_USERS")
                    .description("Can read user data")
                    .build();

            assertThat(permission.getName()).isEqualTo("READ_USERS");
            assertThat(permission.getDescription()).isEqualTo("Can read user data");
        }

        @Test
        void equalsAndHashCode_sameId() {
            Permission p1 = new Permission(1L);
            Permission p2 = new Permission(1L);

            assertThat(p1).isEqualTo(p2);
            assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
        }

        @Test
        void equalsAndHashCode_differentId() {
            Permission p1 = new Permission(1L);
            Permission p2 = new Permission(2L);

            assertThat(p1).isNotEqualTo(p2);
        }

        @Test
        void equalsAndHashCode_nullId() {
            Permission p1 = new Permission();
            Permission p2 = new Permission();

            assertThat(p1).isNotEqualTo(p2);
        }

        @Test
        void equals_withNull() {
            Permission permission = new Permission(1L);
            assertThat(permission).isNotEqualTo(null);
        }

        @Test
        void toStringContainsRelevantFields() {
            Permission permission = Permission.builder()
                    .id(1L)
                    .name("WRITE")
                    .description("Write access")
                    .build();

            String str = permission.toString();
            assertThat(str).contains("1");
            assertThat(str).contains("WRITE");
        }
    }

    @Nested
    class TierConfigEntityTest {

        @Test
        void idOnlyConstructor() {
            TierConfig config = new TierConfig(1L);
            assertThat(config.getId()).isEqualTo(1L);
        }

        @Test
        void builderAndGetters() {
            TierConfig config = TierConfig.builder()
                    .id(1L)
                    .tier(MerchantTier.TIER_1)
                    .dailyTransactionLimit(BigDecimal.valueOf(100000))
                    .singleTransactionLimit(BigDecimal.valueOf(50000))
                    .monthlyTransactionLimit(BigDecimal.valueOf(1000000))
                    .build();

            assertThat(config.getTier()).isEqualTo(MerchantTier.TIER_1);
            assertThat(config.getDailyTransactionLimit()).isEqualByComparingTo(BigDecimal.valueOf(100000));
            assertThat(config.getSingleTransactionLimit()).isEqualByComparingTo(BigDecimal.valueOf(50000));
            assertThat(config.getMonthlyTransactionLimit()).isEqualByComparingTo(BigDecimal.valueOf(1000000));
        }

        @Test
        void equalsAndHashCode_sameId() {
            TierConfig t1 = new TierConfig(1L);
            TierConfig t2 = new TierConfig(1L);

            assertThat(t1).isEqualTo(t2);
            assertThat(t1.hashCode()).isEqualTo(t2.hashCode());
        }

        @Test
        void equalsAndHashCode_differentId() {
            TierConfig t1 = new TierConfig(1L);
            TierConfig t2 = new TierConfig(2L);

            assertThat(t1).isNotEqualTo(t2);
        }

        @Test
        void equalsAndHashCode_nullId() {
            TierConfig t1 = new TierConfig();
            TierConfig t2 = new TierConfig();

            assertThat(t1).isNotEqualTo(t2);
        }

        @Test
        void equals_withNull() {
            TierConfig config = new TierConfig(1L);
            assertThat(config).isNotEqualTo(null);
        }

        @Test
        void toStringContainsRelevantFields() {
            TierConfig config = TierConfig.builder()
                    .id(1L)
                    .tier(MerchantTier.TIER_2)
                    .dailyTransactionLimit(BigDecimal.valueOf(200000))
                    .build();

            String str = config.toString();
            assertThat(str).contains("1");
            assertThat(str).contains("TIER_2");
        }
    }

    @Nested
    class RolePermissionEntityTest {

        @Test
        void idOnlyConstructor() {
            RolePermissionId id = RolePermissionId.builder().roleId(1L).permissionId(2L).build();
            RolePermission rp = new RolePermission(id);
            assertThat(rp.getId()).isEqualTo(id);
        }

        @Test
        void builderAndGetters() {
            RolePermissionId id = RolePermissionId.builder().roleId(1L).permissionId(2L).build();
            Role role = new Role(1L);
            Permission permission = new Permission(2L);

            RolePermission rp = RolePermission.builder()
                    .id(id)
                    .role(role)
                    .permission(permission)
                    .build();

            assertThat(rp.getId()).isEqualTo(id);
            assertThat(rp.getRole()).isEqualTo(role);
            assertThat(rp.getPermission()).isEqualTo(permission);
        }

        @Test
        void equalsAndHashCode_sameId() {
            RolePermissionId id1 = RolePermissionId.builder().roleId(1L).permissionId(2L).build();
            RolePermissionId id2 = RolePermissionId.builder().roleId(1L).permissionId(2L).build();
            RolePermission rp1 = new RolePermission(id1);
            RolePermission rp2 = new RolePermission(id2);

            assertThat(rp1).isEqualTo(rp2);
            assertThat(rp1.hashCode()).isEqualTo(rp2.hashCode());
        }

        @Test
        void equalsAndHashCode_differentId() {
            RolePermissionId id1 = RolePermissionId.builder().roleId(1L).permissionId(2L).build();
            RolePermissionId id2 = RolePermissionId.builder().roleId(1L).permissionId(3L).build();
            RolePermission rp1 = new RolePermission(id1);
            RolePermission rp2 = new RolePermission(id2);

            assertThat(rp1).isNotEqualTo(rp2);
        }

        @Test
        void equalsAndHashCode_nullId() {
            RolePermission rp1 = new RolePermission();
            RolePermission rp2 = new RolePermission();

            assertThat(rp1).isNotEqualTo(rp2);
        }

        @Test
        void equals_withNull() {
            RolePermissionId id = RolePermissionId.builder().roleId(1L).permissionId(2L).build();
            RolePermission rp = new RolePermission(id);
            assertThat(rp).isNotEqualTo(null);
        }

        @Test
        void toStringContainsRelevantFields() {
            RolePermissionId id = RolePermissionId.builder().roleId(1L).permissionId(2L).build();
            RolePermission rp = RolePermission.builder().id(id).build();

            String str = rp.toString();
            assertThat(str).contains("id=");
        }
    }

    @Nested
    class RolePermissionIdTest {

        @Test
        void builderAndGetters() {
            RolePermissionId id = RolePermissionId.builder()
                    .roleId(1L)
                    .permissionId(2L)
                    .build();

            assertThat(id.getRoleId()).isEqualTo(1L);
            assertThat(id.getPermissionId()).isEqualTo(2L);
        }

        @Test
        void equalsAndHashCode_sameValues() {
            RolePermissionId id1 = RolePermissionId.builder().roleId(1L).permissionId(2L).build();
            RolePermissionId id2 = RolePermissionId.builder().roleId(1L).permissionId(2L).build();

            assertThat(id1).isEqualTo(id2);
            assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
        }

        @Test
        void equalsAndHashCode_differentRoleId() {
            RolePermissionId id1 = RolePermissionId.builder().roleId(1L).permissionId(2L).build();
            RolePermissionId id2 = RolePermissionId.builder().roleId(3L).permissionId(2L).build();

            assertThat(id1).isNotEqualTo(id2);
        }

        @Test
        void equalsAndHashCode_differentPermissionId() {
            RolePermissionId id1 = RolePermissionId.builder().roleId(1L).permissionId(2L).build();
            RolePermissionId id2 = RolePermissionId.builder().roleId(1L).permissionId(3L).build();

            assertThat(id1).isNotEqualTo(id2);
        }

        @Test
        void equalsAndHashCode_nullValues() {
            RolePermissionId id1 = new RolePermissionId();
            RolePermissionId id2 = new RolePermissionId();

            assertThat(id1).isNotEqualTo(id2);
        }

        @Test
        void equals_withNull() {
            RolePermissionId id = RolePermissionId.builder().roleId(1L).permissionId(2L).build();
            assertThat(id).isNotEqualTo(null);
        }

        @Test
        void toStringContainsRelevantFields() {
            RolePermissionId id = RolePermissionId.builder().roleId(1L).permissionId(2L).build();

            String str = id.toString();
            assertThat(str).contains("1");
            assertThat(str).contains("2");
        }
    }
}
