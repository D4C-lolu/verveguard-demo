package com.interswitch.verveguarddemo.services;

import com.interswitch.verveguard.api.FraudDecision;
import com.interswitch.verveguard.api.FraudEvaluator;
import com.interswitch.verveguard.api.model.FraudContext;
import com.interswitch.verveguard.api.model.FraudResult;
import com.interswitch.verveguard.api.model.GateResult;
import com.interswitch.verveguarddemo.context.PrefetchedFraudDataProvider;
import com.interswitch.verveguarddemo.dao.FraudDao;
import com.interswitch.verveguarddemo.models.enums.FraudStatus;
import com.interswitch.verveguarddemo.models.projections.FraudEvaluationContext;
import com.interswitch.verveguarddemo.models.request.FraudEvaluationRequest;
import com.interswitch.verveguarddemo.models.response.FraudAttemptResponse;
import com.interswitch.verveguarddemo.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private final FraudDao fraudDao;
    private final FraudAttemptLoggerService fraudLogger;
    private final FraudConsequenceService fraudConsequenceService;
    private final FraudEvaluator fraudEvaluator;
    private final PrefetchedFraudDataProvider fraudDataProvider;
    private final UserIpHistoryService userIpHistoryService;

    /**
     * Called from merchant-facing endpoints — merchantId from security context.
     */
    @Transactional
    public FraudStatus evaluate(FraudEvaluationRequest request, String ipAddress) {
        Long merchantId = SecurityUtil.getCurrentUserId();
        return evaluate(buildContext(request, merchantId, ipAddress));
    }

    /**
     * Called from admin/internal endpoints — merchantId explicit on request.
     */
    @Transactional
    public FraudStatus evaluateForMerchant(FraudEvaluationRequest request, Long merchantId, String ipAddress) {
        return evaluate(buildContext(request, merchantId, ipAddress));
    }

    private FraudStatus evaluate(FraudEvaluationContext ctx) {
        String cardHash = DigestUtils.sha256Hex(ctx.cardNumber());

        try {
            fraudDataProvider.prefetch(cardHash, ctx.ipAddress());

            FraudResult result = performEvaluation(ctx, cardHash);
            FraudStatus status = mapDecision(result.decision());
            List<String> flags = extractFlags(result);

            handlePostEvaluationActions(ctx, cardHash, status, flags);

            return status;
        } finally {
            fraudDataProvider.clear();
        }
    }

    private FraudEvaluationContext buildContext(FraudEvaluationRequest request,
                                                Long merchantId, String ipAddress) {
        return new FraudEvaluationContext(
                merchantId,
                UUID.randomUUID().toString(),
                request.amount(),
                request.currency(),
                request.cardNumber(),
                ipAddress,
                OffsetDateTime.now()
        );
    }

    private FraudResult performEvaluation(FraudEvaluationContext ctx, String cardHash) {
        Set<String> recentIps = userIpHistoryService.getRecentIps(String.valueOf(ctx.merchantId()));

        FraudContext fraudContext = FraudContext.builder()
                .transactionId(ctx.transactionId())
                .cardHash(cardHash)
                .ipAddress(ctx.ipAddress())
                .amount(ctx.amount())
                .currency(ctx.currency())
                .transactionTime(ctx.transactionTime().toInstant())
                .lastKnownIpAddresses(recentIps)
                .build();

        return fraudEvaluator.evaluate(fraudContext);
    }

    private void handlePostEvaluationActions(FraudEvaluationContext ctx, String cardHash,
                                             FraudStatus status, List<String> flags) {
        userIpHistoryService.recordIpAsync(String.valueOf(ctx.merchantId()), ctx.ipAddress());
        fraudLogger.logAttempt(cardHash, ctx, status, flags);

        if (status != FraudStatus.CLEAN) {
            applyConsequences(ctx, cardHash, status, flags);
        }
    }

    private void applyConsequences(FraudEvaluationContext ctx, String cardHash,
                                   FraudStatus status, List<String> flags) {
        try {
            fraudConsequenceService.applyConsequences(ctx, cardHash, status, flags);
        } catch (Exception e) {
            log.error("Failed to apply fraud consequences for merchant={}", ctx.merchantId(), e);
        }
    }

    private FraudStatus mapDecision(FraudDecision decision) {
        return switch (decision) {
            case ALLOW  -> FraudStatus.CLEAN;
            case REVIEW -> FraudStatus.SUSPICIOUS;
            case BLOCK  -> FraudStatus.BLOCKED;
        };
    }

    private List<String> extractFlags(FraudResult result) {
        return result.gateResults().stream()
                .filter(gr -> gr.score() > 0 || gr.hardBlock())
                .map(GateResult::gateName)
                .toList();
    }

    public Page<FraudAttemptResponse> getFraudAttempts(int page, int size) {
        return fraudDao.getFraudAttempts(page, size);
    }
}