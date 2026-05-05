package com.interswitch.verveguarddemo.services;

import com.interswitch.verveguard.api.FraudDecision;
import com.interswitch.verveguard.api.FraudEvaluator;
import com.interswitch.verveguard.api.model.FraudContext;
import com.interswitch.verveguard.api.model.FraudResult;
import com.interswitch.verveguard.api.model.GateResult;
import com.interswitch.verveguarddemo.dao.FraudDao;
import com.interswitch.verveguarddemo.fraud.FraudDataSnapshot;
import com.interswitch.verveguarddemo.fraud.PrefetchedFraudDataProvider;
import com.interswitch.verveguarddemo.fraud.UserIpHistoryService;
import com.interswitch.verveguarddemo.models.enums.FraudStatus;
import com.interswitch.verveguarddemo.models.projections.FraudEvaluationContext;
import com.interswitch.verveguarddemo.models.response.FraudAttemptResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

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

    public String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Overloaded evaluate method that explicitly accepts a merchantId.
     */
    @Transactional
    public FraudStatus evaluate(FraudEvaluationContext ctx, Long merchantId) {
        String cardHash = DigestUtils.sha256Hex(ctx.cardNumber());

        try {
            fraudDataProvider.prefetch(ctx.accountNumber(), cardHash, ctx.ipAddress());

            FraudResult result = performEvaluation(ctx, cardHash);
            FraudStatus status = mapDecision(result.decision());
            List<String> flags = extractFlags(result);

            handlePostEvaluationActions(ctx, cardHash, status, flags, merchantId);

            return status;
        } finally {
            fraudDataProvider.clear();
        }
    }

    /**
     * Original evaluate method - now delegates to the new implementation.
     */
    @Transactional
    public FraudStatus evaluate(FraudEvaluationContext ctx) {
        // Attempt to extract merchantId from provider if not provided explicitly
        FraudDataSnapshot snapshot = fraudDataProvider.getSnapshot();
        Long merchantId = (snapshot != null) ? snapshot.merchantId() : null;

        return evaluate(ctx, merchantId);
    }

    private FraudResult performEvaluation(FraudEvaluationContext ctx, String cardHash) {
        Set<String> recentIps = userIpHistoryService.getRecentIps(ctx.accountNumber());

        FraudContext fraudContext = FraudContext.builder()
                .transactionId(ctx.transactionId())
                .accountIdentifier(ctx.accountNumber())
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
                                             FraudStatus status, List<String> flags, Long merchantId) {

        // Update user IP history asynchronously
        userIpHistoryService.recordIpAsync(ctx.accountNumber(), ctx.ipAddress());

        // Log the attempt
        fraudLogger.logAttempt(cardHash, ctx, merchantId, status, flags);

        // Apply consequences if the status isn't clean
        if (status != FraudStatus.CLEAN) {
            applyConsequences(ctx, cardHash, status, flags);
        }
    }

    private FraudStatus mapDecision(FraudDecision decision) {
        return switch (decision) {
            case ALLOW -> FraudStatus.CLEAN;
            case REVIEW -> FraudStatus.SUSPICIOUS;
            case BLOCK -> FraudStatus.BLOCKED;
        };
    }

    private List<String> extractFlags(FraudResult result) {
        return result.gateResults().stream()
                .filter(gr -> gr.score() > 0 || gr.hardBlock())
                .map(GateResult::gateName)
                .toList();
    }

    public void applyConsequences(FraudEvaluationContext ctx, String cardHash, FraudStatus status, List<String> flags) {
        try {
            fraudConsequenceService.applyConsequences(ctx, cardHash, status, flags);
        } catch (Exception e) {
            log.error("Failed to apply fraud consequences for account: {}", ctx.accountNumber(), e);
        }
    }

    public Page<FraudAttemptResponse> getFraudAttempts(int page, int size) {
        int offset = (page - 1) * size;
        List<FraudAttemptResponse> attempts = fraudDao.getFraudAttempts(size, offset);
        long total = fraudDao.countFraudAttempts();
        return new PageImpl<>(attempts, PageRequest.of(page - 1, size), total);
    }
}