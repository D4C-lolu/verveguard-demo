package com.interswitch.verveguarddemo.models.projections;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectionsTest {

    @Test
    void fraudEvaluationData_recordAccessors() {
        var data = new FraudEvaluationData(
                true,
                5,
                BigDecimal.valueOf(10000),
                123L
        );

        assertThat(data.isBlacklisted()).isTrue();
        assertThat(data.velocityCount()).isEqualTo(5);
        assertThat(data.transactionLimit()).isEqualByComparingTo(BigDecimal.valueOf(10000));
        assertThat(data.merchantId()).isEqualTo(123L);
    }

    @Test
    void fraudEvaluationData_equalsAndHashCode() {
        var data1 = new FraudEvaluationData(true, 5, BigDecimal.valueOf(10000), 123L);
        var data2 = new FraudEvaluationData(true, 5, BigDecimal.valueOf(10000), 123L);

        assertThat(data1).isEqualTo(data2);
        assertThat(data1.hashCode()).isEqualTo(data2.hashCode());
    }

    @Test
    void fraudEvaluationData_toString() {
        var data = new FraudEvaluationData(false, 0, BigDecimal.ZERO, 1L);
        assertThat(data.toString()).contains("FraudEvaluationData");
    }

    @Test
    void staticFraudData_recordAccessors() {
        var data = new StaticFraudData(
                true,
                false,
                BigDecimal.valueOf(50000),
                456L
        );

        assertThat(data.isCardBlocked()).isTrue();
        assertThat(data.isMerchantBlacklisted()).isFalse();
        assertThat(data.transactionLimit()).isEqualByComparingTo(BigDecimal.valueOf(50000));
        assertThat(data.merchantId()).isEqualTo(456L);
    }

    @Test
    void staticFraudData_equalsAndHashCode() {
        var data1 = new StaticFraudData(true, false, BigDecimal.valueOf(50000), 456L);
        var data2 = new StaticFraudData(true, false, BigDecimal.valueOf(50000), 456L);

        assertThat(data1).isEqualTo(data2);
        assertThat(data1.hashCode()).isEqualTo(data2.hashCode());
    }

    @Test
    void staticFraudData_toString() {
        var data = new StaticFraudData(false, false, BigDecimal.ZERO, 1L);
        assertThat(data.toString()).contains("StaticFraudData");
    }

    @Test
    void merchantAlertInfo_isInterface() {
        // MerchantAlertInfo is an interface, test that implementations work
        MerchantAlertInfo info = new MerchantAlertInfo() {
            @Override
            public String getEmail() {
                return "test@example.com";
            }

            @Override
            public String getFullname() {
                return "John Doe";
            }
        };

        assertThat(info.getEmail()).isEqualTo("test@example.com");
        assertThat(info.getFullname()).isEqualTo("John Doe");
    }
}
