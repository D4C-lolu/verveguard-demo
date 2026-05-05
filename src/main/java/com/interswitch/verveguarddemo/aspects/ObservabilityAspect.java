package com.interswitch.verveguarddemo.aspects;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Aspect
@Component
public class ObservabilityAspect {

    @Around("@within(com.interswitch.verveguarddemo.annotation.Observe)")
    public Object observe(ProceedingJoinPoint pjp) throws Throwable {

        String className = pjp.getTarget().getClass().getSimpleName();
        String method = pjp.getSignature().getName();

        String traceId = MDC.get("traceId");
        boolean addedLocally = false;

        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
            addedLocally = true;
            MDC.put("traceId", traceId);
        }

        MDC.put("spanId", UUID.randomUUID().toString());

        log.info("→ {}.{}", className, method);
        long start = System.currentTimeMillis();

        try {
            Object result = pjp.proceed();
            log.info("← {}.{} completed in {}ms", className, method, System.currentTimeMillis() - start);
            return result;
        } catch (Exception ex) {
            log.error("← {}.{} failed in {}ms — {}", className, method, System.currentTimeMillis() - start, ex.getMessage());
            throw ex;
        } finally {
            MDC.remove("spanId");
            if (addedLocally) MDC.remove("traceId");
        }
    }
}