package com.interswitch.verveguarddemo.services;

import com.interswitch.verveguarddemo.dao.BlacklistDao;
import com.interswitch.verveguarddemo.models.enums.FraudStatus;
import com.interswitch.verveguarddemo.models.projections.FraudEvaluationContext;
import com.interswitch.verveguarddemo.models.projections.MerchantAlertInfo;
import com.interswitch.verveguarddemo.repositories.MerchantRepository;
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
public class FraudConsequenceService {

    private final BlacklistDao blacklistDao;
    private final MerchantRepository merchantRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applyConsequences(FraudEvaluationContext ctx, String cardHash, FraudStatus status, List<String> flags) {
        if (status == FraudStatus.BLOCKED) {
            handleBlockedTransaction(ctx, cardHash, flags);
        } else if (status == FraudStatus.SUSPICIOUS) {
            handleSuspiciousTransaction(ctx, cardHash, flags);
        }
    }

    private void handleBlockedTransaction(FraudEvaluationContext ctx, String cardHash, List<String> flags) {
        boolean cardBlocked = blockCard(cardHash);
        simulateFraudAlert(ctx, cardHash, FraudStatus.BLOCKED, flags, cardBlocked);
    }

    private void handleSuspiciousTransaction(FraudEvaluationContext ctx, String cardHash, List<String> flags) {
        simulateFraudAlert(ctx, cardHash, FraudStatus.SUSPICIOUS, flags, false);
    }

    private boolean blockCard(String cardHash) {
        try {
            boolean blocked = blacklistDao.blockByHash(cardHash);
            if (blocked) {
                log.warn("[FRAUD] Card blocked. hash={}", maskHash(cardHash));
            } else {
                log.debug("[FRAUD] Card not blocked — may already be blocked or not found. hash={}", maskHash(cardHash));
            }
            return blocked;
        } catch (Exception e) {
            log.error("[FRAUD] Failed to block card. hash={}", maskHash(cardHash), e);
            return false;
        }
    }

    private void simulateFraudAlert(FraudEvaluationContext ctx, String cardHash, FraudStatus status, List<String> flags, boolean cardWasBlocked) {
        try {
            MerchantAlertInfo info = merchantRepository.findAlertInfoByCardHash(cardHash).orElse(null);

            if (info == null) {
                log.warn("[FRAUD] Merchant info not found for hash={}. Skipping alert.", maskHash(cardHash));
                return;
            }

            log.info("""
                            [FRAUD ALERT - SIMULATED EMAIL]
                            To      : {} <{}>
                            Subject : {}
                            Status  : {}
                            Amount  : {} {}
                            IP      : {}
                            Time    : {}
                            Flags   : {}
                            Card    : {}
                            """,
                    info.getFullname(), info.getEmail(),
                    status == FraudStatus.BLOCKED
                            ? "[URGENT] Transaction Blocked - Fraud Alert"
                            : "[WARNING] Suspicious Transaction Detected",
                    status,
                    ctx.currency(), ctx.amount(),
                    ctx.ipAddress(),
                    ctx.transactionTime(),
                    flags,
                    cardWasBlocked ? "BLOCKED" : "NOT BLOCKED"
            );
        } catch (Exception e) {
            log.error("[FRAUD] Failed to simulate alert for hash={}", maskHash(cardHash), e);
        }
    }

    private String maskHash(String hash) {
        if (hash == null || hash.length() < 8) return "****";
        return hash.substring(0, 8) + "...";
    }
}