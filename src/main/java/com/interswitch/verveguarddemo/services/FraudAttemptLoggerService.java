package com.interswitch.verveguarddemo.services;

import com.interswitch.verveguarddemo.annotation.Observed;
import com.interswitch.verveguarddemo.dao.FraudDao;
import com.interswitch.verveguarddemo.models.enums.FraudStatus;
import com.interswitch.verveguarddemo.models.projections.FraudAttemptRecord;
import com.interswitch.verveguarddemo.models.projections.FraudEvaluationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudAttemptLoggerService {

    private final FraudDao fraudDao;

    @Observed
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAttempt(String cardHash, FraudEvaluationContext ctx, FraudStatus status, List<String> flags) {
        try {
            fraudDao.insertFraudAttempt(new FraudAttemptRecord(
                    cardHash,
                    ctx.merchantId(),
                    ctx.ipAddress(),
                    ctx.amount(),
                    ctx.currency(),
                    status,
                    flags
            ));
        } catch (Exception e) {
            log.error("Failed to log fraud attempt for merchant={} ip={}", ctx.merchantId(), ctx.ipAddress(), e);
        }
    }
}