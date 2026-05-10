package com.interswitch.verveguarddemo.services;

import com.interswitch.verveguarddemo.annotation.Observed;
import com.interswitch.verveguarddemo.constants.CacheId;
import com.interswitch.verveguarddemo.dao.FraudDao;
import com.interswitch.verveguarddemo.models.projections.StaticFraudData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDataService {

    private final FraudDao fraudDao;

    @Observed
    @Cacheable(value = CacheId.Names.FRAUD_EVALUATION, key = "#cardHash")
    public StaticFraudData getEvaluationData(String cardHash) {
        return fraudDao.getEvaluationData(cardHash);
    }

    @Observed
    @CacheEvict(value = CacheId.Names.FRAUD_EVALUATION, key = "#cardHash")
    public void evict(String cardHash) {
        log.debug("Evicted fraud eval cache for cardHash={}", cardHash);
    }
}