package com.interswitch.verveguarddemo.entities.audit;

import com.interswitch.verveguarddemo.entities.User;
import com.interswitch.verveguarddemo.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuditClassesTest {

    @Nested
    class FullAuditTest {

        @BeforeEach
        void setUp() {
            // Set up a mocked superadmin user in security context
            User mockUser = mock(User.class);
            when(mockUser.getId()).thenReturn(1L);
            when(mockUser.getEmail()).thenReturn("superadmin@verveguard.com");

            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(new UserPrincipal(mockUser), null, List.of())
            );
        }

        @AfterEach
        void tearDown() {
            SecurityContextHolder.clearContext();
        }

        @Test
        void gettersAndSetters() {
            FullAudit audit = new FullAudit();
            OffsetDateTime now = OffsetDateTime.now();

            audit.setCreatedAt(now);
            audit.setUpdatedAt(now.plusHours(1));
            audit.setCreatedBy(1L);
            audit.setUpdatedBy(2L);
            audit.setDeletedAt(now.plusDays(1));
            audit.setDeletedBy(3L);

            assertThat(audit.getCreatedAt()).isEqualTo(now);
            assertThat(audit.getUpdatedAt()).isEqualTo(now.plusHours(1));
            assertThat(audit.getCreatedBy()).isEqualTo(1L);
            assertThat(audit.getUpdatedBy()).isEqualTo(2L);
            assertThat(audit.getDeletedAt()).isEqualTo(now.plusDays(1));
            assertThat(audit.getDeletedBy()).isEqualTo(3L);
        }

        @Test
        void isNotDeleted_returnsTrueWhenDeletedAtIsNull() {
            FullAudit audit = new FullAudit();

            assertThat(audit.isNotDeleted()).isTrue();
        }

        @Test
        void isNotDeleted_returnsFalseWhenDeletedAtIsSet() {
            FullAudit audit = new FullAudit();
            audit.setDeletedAt(OffsetDateTime.now());

            assertThat(audit.isNotDeleted()).isFalse();
        }

        @Test
        void softDelete_setsDeletedAt() {
            FullAudit audit = new FullAudit();

            audit.softDelete();

            assertThat(audit.getDeletedAt()).isNotNull();
            assertThat(audit.isNotDeleted()).isFalse();
        }

        @Test
        void softDelete_preservesExistingDeletedBy() {
            FullAudit audit = new FullAudit();
            audit.setDeletedBy(99L);

            audit.softDelete();

            assertThat(audit.getDeletedBy()).isEqualTo(99L);
        }

        @Test
        void softDelete_setsDeletedByFromSecurityContext() {
            // Security context is set up in @BeforeEach with user ID 1L
            FullAudit audit = new FullAudit();
            audit.softDelete();

            assertThat(audit.getDeletedAt()).isNotNull();
            assertThat(audit.getDeletedBy()).isEqualTo(1L);
        }
    }

    @Nested
    class MutableAuditTest {

        @Test
        void gettersAndSetters() {
            MutableAudit audit = new MutableAudit();
            OffsetDateTime now = OffsetDateTime.now();

            audit.setCreatedAt(now);
            audit.setUpdatedAt(now.plusHours(1));
            audit.setCreatedBy(1L);
            audit.setUpdatedBy(2L);

            assertThat(audit.getCreatedAt()).isEqualTo(now);
            assertThat(audit.getUpdatedAt()).isEqualTo(now.plusHours(1));
            assertThat(audit.getCreatedBy()).isEqualTo(1L);
            assertThat(audit.getUpdatedBy()).isEqualTo(2L);
        }

        @Test
        void defaultConstructor() {
            MutableAudit audit = new MutableAudit();

            assertThat(audit.getCreatedAt()).isNull();
            assertThat(audit.getUpdatedAt()).isNull();
            assertThat(audit.getCreatedBy()).isNull();
            assertThat(audit.getUpdatedBy()).isNull();
        }
    }

    @Nested
    class CreatedAuditTest {

        @Test
        void gettersAndSetters() {
            CreatedAudit audit = new CreatedAudit();
            OffsetDateTime now = OffsetDateTime.now();

            audit.setCreatedAt(now);
            audit.setCreatedBy(1L);

            assertThat(audit.getCreatedAt()).isEqualTo(now);
            assertThat(audit.getCreatedBy()).isEqualTo(1L);
        }

        @Test
        void defaultConstructor() {
            CreatedAudit audit = new CreatedAudit();

            assertThat(audit.getCreatedAt()).isNull();
            assertThat(audit.getCreatedBy()).isNull();
        }
    }

    @Nested
    class TimestampAuditTest {

        @Test
        void gettersAndSetters() {
            TimestampAudit audit = new TimestampAudit();
            OffsetDateTime now = OffsetDateTime.now();

            audit.setCreatedAt(now);
            audit.setUpdatedAt(now.plusHours(1));

            assertThat(audit.getCreatedAt()).isEqualTo(now);
            assertThat(audit.getUpdatedAt()).isEqualTo(now.plusHours(1));
        }

        @Test
        void defaultConstructor() {
            TimestampAudit audit = new TimestampAudit();

            assertThat(audit.getCreatedAt()).isNull();
            assertThat(audit.getUpdatedAt()).isNull();
        }
    }
}
