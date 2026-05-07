package com.interswitch.verveguarddemo.context;

import com.interswitch.verveguard.api.GeoIpService;
import com.interswitch.verveguard.core.AbstractFraudDataProvider;
import com.interswitch.verveguarddemo.dao.FraudDao;
import com.interswitch.verveguarddemo.models.projections.FraudDataSnapshot;
import com.interswitch.verveguarddemo.models.projections.StaticFraudData;
import com.interswitch.verveguarddemo.services.FraudDataService;
import com.interswitch.verveguarddemo.services.RateLimiterService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

@Slf4j
@Component
public class PrefetchedFraudDataProvider extends AbstractFraudDataProvider {

    private static final ThreadLocal<FraudDataSnapshot> SNAPSHOT_HOLDER = new ThreadLocal<>();
    private final FraudDataService fraudDataService;
    private final RateLimiterService rateLimiterService;
    private final FraudDao fraudDao;
    private final VelocityCounter velocityCounter;

    public PrefetchedFraudDataProvider(GeoIpService geoIpService,
                                       FraudDataService fraudDataService,
                                       RateLimiterService rateLimiterService,
                                       FraudDao fraudDao,
                                       VelocityCounter velocityCounter) {
        super(geoIpService);
        this.fraudDataService = fraudDataService;
        this.rateLimiterService = rateLimiterService;
        this.fraudDao = fraudDao;
        this.velocityCounter = velocityCounter;
    }

    public void prefetch(String cardHash, String ipAddress) {
        StaticFraudData data = fraudDataService.getEvaluationData(cardHash);
        boolean isRateLimited = rateLimiterService.isRateLimited(ipAddress);

        SNAPSHOT_HOLDER.set(new FraudDataSnapshot(
                data.isCardBlocked(),
                data.isMerchantBlacklisted(),
                isRateLimited,
                data.transactionLimit()
        ));
    }

    public void clear() {
        SNAPSHOT_HOLDER.remove();
    }

    @Override
    public boolean isBlacklisted(String cardNumber) {
        FraudDataSnapshot snapshot = SNAPSHOT_HOLDER.get();
        if (snapshot == null) {
            log.warn("No prefetched snapshot, falling back to DB for blacklist/blocked check");
            StaticFraudData data = fraudDao.getEvaluationData(DigestUtils.sha256Hex(cardNumber));
            return data.isCardBlocked() || data.isMerchantBlacklisted();
        }
        return snapshot.isCardBlocked() || snapshot.isMerchantBlacklisted();
    }

    @Override
    public boolean isRateLimited(String ipAddress) {
        FraudDataSnapshot snapshot = SNAPSHOT_HOLDER.get();
        if (snapshot == null) return rateLimiterService.isRateLimited(ipAddress);
        return snapshot.isRateLimited();
    }

    @Override
    public int getVelocityCount(String cardNumber, Duration window) {
        return velocityCounter.count(cardNumber, OffsetDateTime.now().minus(window));
    }

    @Override
    public Optional<BigDecimal> getTransactionLimit(String cardNumber) {
        FraudDataSnapshot snapshot = SNAPSHOT_HOLDER.get();
        if (snapshot == null) {
            log.warn("No prefetched snapshot, falling back to DB for transaction limit");
            return fraudDao.getTransactionLimitByCardNumber(DigestUtils.sha256Hex(cardNumber));
        }
        return Optional.ofNullable(snapshot.transactionLimit());
    }
}