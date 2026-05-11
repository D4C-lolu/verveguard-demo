# Verveguard Fraud Detection System

## Technical Documentation

---

## 1. Overview

Verveguard is a **real-time fraud detection sidecar** designed to intercept and evaluate payment transactions before they reach the core payment switch. It acts as a protective layer that scores each transaction against configurable fraud rules and returns an immediate decision: **ALLOW**, **REVIEW**, or **BLOCK**.

### Key Characteristics

- **Latency-sensitive**: Target <100ms evaluation time
- **Pluggable architecture**: Gates can be enabled/disabled via configuration
- **Fail-fast design**: Hard blocks exit immediately, soft flags accumulate scores
- **Hybrid persistence**: JPA for relational data, JDBC for high-speed writes, Redis for velocity tracking

---

## 2. Architecture

### High-Level Flow

```
┌─────────────┐     ┌──────────────────┐     ┌─────────────────┐
│   Payment   │────▶│    Verveguard    │────▶│  Core Payment   │
│   Request   │     │     Sidecar      │     │     Switch      │
└─────────────┘     └──────────────────┘     └─────────────────┘
                            │
                    ┌───────┴───────┐
                    ▼               ▼
              ┌──────────┐   ┌──────────┐
              │ Postgres │   │  Redis   │
              │  (JPA/   │   │(velocity)│
              │  JDBC)   │   │          │
              └──────────┘   └──────────┘
```

### Core Components

| Component | Responsibility |
|-----------|----------------|
| `FraudEvaluator` | Main entry point for fraud evaluation |
| `FraudPipeline` | Orchestrates gate execution in order |
| `FraudGate` | Individual fraud rule (blacklist, velocity, etc.) |
| `FraudDataProvider` | Abstraction for data access (DB, cache, external APIs) |
| `FraudContext` | Immutable transaction context passed through gates |

---

## 3. The Gate System

Gates are individual fraud detection rules. Each gate evaluates the transaction and returns:
- A **score** (0-100)
- An optional **hard block** flag
- A **reason code** and **detail** for audit

### Gate Interface

```java
public interface FraudGate {
    String getName();
    int getOrder();                    // Lower = runs first
    boolean isHardBlockCapable();      // Can this gate hard-block?
    GateResult evaluate(FraudContext ctx, FraudDataProvider data);
}
```

### Available Gates

| Gate | Order | Type | Description |
|------|-------|------|-------------|
| `BlacklistGate` | 1 | Hard Block | Blocks blacklisted merchants/cards |
| `RateLimitGate` | 2 | Hard Block | Blocks IPs exceeding request limits |
| `LocationAnomalyGate` | 4 | Soft Flag | Detects impossible travel patterns |
| `VelocityGate` | 10 | Soft Flag | Flags high transaction frequency |
| `TransactionLimitGate` | 11 | Soft Flag | Flags amounts exceeding tier limits |
| `TimeWindowGate` | 12 | Soft Flag | Flags transactions outside business hours |

### Gate Result Model

```java
public record GateResult(
        String gateName,
        int score,
        String reasonCode,
        String reasonDetail,
        boolean hardBlock
) {
    // Factory methods
    public static GateResult pass(String gateName);
    public static GateResult flag(String gateName, int score, String code, String detail);
    public static GateResult block(String gateName, String code, String detail);
}
```

---

## 4. The Pipeline

The `FraudPipeline` implements a **fail-fast** evaluation strategy:

```java
public FraudResult evaluate(FraudContext ctx) {
    List<GateResult> results = new ArrayList<>();

    // 1. Run hard-block gates first (fail-fast)
    for (FraudGate gate : hardBlockGates) {
        GateResult result = gate.evaluate(ctx, dataProvider);
        results.add(result);
        if (result.hardBlock()) {
            return buildResult(FraudDecision.BLOCK, results, start);
        }
    }

    // 2. Run soft gates (accumulate scores)
    for (FraudGate gate : softGates) {
        results.add(gate.evaluate(ctx, dataProvider));
    }

    // 3. Calculate total score and decide
    int totalScore = results.stream().mapToInt(GateResult::score).sum();
    FraudDecision decision = decide(totalScore);

    return buildResult(decision, results, start);
}

private FraudDecision decide(int score) {
    if (score >= blockThreshold) return FraudDecision.BLOCK;  // Default: 70
    if (score >= reviewThreshold) return FraudDecision.REVIEW; // Default: 30
    return FraudDecision.ALLOW;
}
```

### Decision Thresholds

| Total Score | Decision | Action |
|-------------|----------|--------|
| 0-29 | `ALLOW` | Transaction proceeds normally |
| 30-69 | `REVIEW` | Transaction proceeds but flagged for review |
| 70+ | `BLOCK` | Transaction rejected |

---

## 5. Configuration

Verveguard is fully configurable via `application.properties`:

```properties
# Master switch
verveguard.enabled=true

# Decision thresholds
verveguard.block-threshold=70
verveguard.review-threshold=30

# Individual gates
verveguard.blacklist.enabled=true
verveguard.rate-limit.enabled=true

verveguard.velocity.enabled=true
verveguard.velocity.threshold=3          # Max transactions...
verveguard.velocity.window-seconds=60    # ...per this window
verveguard.velocity.score=30             # Score when exceeded

verveguard.transaction-limit.enabled=true
verveguard.transaction-limit.score=25

verveguard.time-window.enabled=true
verveguard.time-window.start-hour=6      # Business hours start
verveguard.time-window.end-hour=22       # Business hours end
verveguard.time-window.score=10

verveguard.location-anomaly.enabled=true
verveguard.location-anomaly.anomaly-threshold=60
verveguard.location-anomaly.score=35

# GeoIP database for location detection
verveguard.geo-ip.database-path=/opt/geoip/GeoLite2-City.mmdb
```

### Auto-Configuration

Gates are automatically wired via Spring's `@ConditionalOnProperty`:

```java
@Bean
@ConditionalOnProperty(prefix = "verveguard.velocity", name = "enabled", matchIfMissing = true)
public VelocityGate velocityGate(VerveguardProperties props) {
    var cfg = props.getVelocity();
    return new VelocityGate(
            cfg.getThreshold(),
            Duration.ofSeconds(cfg.getWindowSeconds()),
            cfg.getScore()
    );
}
```

---

## 6. Data Access Strategy

### The Challenge

Fraud evaluation is latency-critical. A naive implementation would make multiple DB queries per gate:
- Is merchant blacklisted? (1 query)
- What's the transaction limit? (1 query)
- Is card blocked? (1 query)

At 1000 req/s, this becomes 3000+ queries/second for static data that rarely changes.

### The Solution: Prefetched Data Provider

```java
@Component
public class PrefetchedFraudDataProvider extends AbstractFraudDataProvider {

    private static final ThreadLocal<FraudDataSnapshot> SNAPSHOT_HOLDER = new ThreadLocal<>();

    public void prefetch(String cardHash, String ipAddress) {
        // Single query fetches ALL static data
        StaticFraudData data = fraudDataService.getEvaluationData(cardHash);
        boolean isRateLimited = rateLimiterService.isRateLimited(ipAddress);

        SNAPSHOT_HOLDER.set(new FraudDataSnapshot(
                data.isCardBlocked(),
                data.isMerchantBlacklisted(),
                isRateLimited,
                data.transactionLimit()
        ));
    }

    @Override
    public boolean isBlacklisted(String cardNumber) {
        FraudDataSnapshot snapshot = SNAPSHOT_HOLDER.get();
        return snapshot.isCardBlocked() || snapshot.isMerchantBlacklisted();
    }

    // ... other methods read from snapshot
}
```

### Hybrid Persistence

| Data Type | Storage | Why |
|-----------|---------|-----|
| Merchants, Cards, Accounts | JPA (Postgres) | Relational integrity, complex queries |
| Fraud attempt logs | JDBC (stored procedures) | High-speed inserts, no ORM overhead |
| Velocity counts | Redis sorted sets | Sub-millisecond reads, automatic expiry |
| Blacklist lookups | Caffeine cache | Hot data, avoids repeated DB queries |

---

## 7. Caching Strategy

### Tiered Cache (L1 + L2)

For high-throughput scenarios, Verveguard uses a two-tier cache:

```java
public class TieredCache implements Cache {

    private final Cache l1;  // Caffeine (in-process, fastest)
    private final Cache l2;  // Redis (distributed, shared across instances)

    @Override
    public ValueWrapper get(Object key) {
        // Try L1 first
        ValueWrapper v = l1.get(key);
        if (v != null) return v;

        // Fall back to L2
        v = l2.get(key);
        if (v != null) {
            l1.put(key, v.get()); // Backfill L1
        }
        return v;
    }

    @Override
    public void put(Object key, Object value) {
        l1.put(key, value);
        l2.put(key, value);  // Write-through
    }
}
```

### Cache Stampede Prevention

When the cache is empty and many requests arrive simultaneously, they all try to load from DB — a "thundering herd" problem. The TieredCache uses per-key locking:

```java
public <T> T get(Object key, Callable<T> valueLoader) {
    // Check caches first...

    // True miss — only one thread loads per key
    Object lock = keyLocks.computeIfAbsent(key, k -> new Object());
    synchronized (lock) {
        // Double-check after acquiring lock
        // Load from DB
        // Populate both caches
    }
}
```

### Measuring Cache Performance

Run the cache benchmark to measure actual speedup on your hardware:

```bash
k6 run test-scripts/cache-benchmark.js
```

The benchmark compares cold (cache miss) vs warm (cache hit) latency and reports the speedup ratio. Cache stats are also available via `/actuator/health`.

---

## 8. Velocity Tracking with Redis

Transaction velocity (how many transactions per time window) requires:
- Fast writes during evaluation
- Fast reads during evaluation
- Automatic expiry of old data

Redis sorted sets are ideal:

```java
@Component
public class VelocityCounter {

    private static final String KEY_PREFIX = "fraud:velocity:";
    private static final Duration RETENTION = Duration.ofHours(25);

    @Async  // Non-blocking write
    public void record(String cardNumber, String transactionId) {
        redisTemplate.opsForZSet().add(
            toKey(cardNumber),
            transactionId,
            Instant.now().toEpochMilli()  // Score = timestamp
        );
        redisTemplate.expire(toKey(cardNumber), RETENTION);
    }

    public int count(String cardNumber, OffsetDateTime since) {
        // Count entries with score (timestamp) >= since
        Long count = redisTemplate.opsForZSet().count(
            toKey(cardNumber),
            since.toInstant().toEpochMilli(),
            Double.MAX_VALUE
        );
        return count != null ? count.intValue() : 0;
    }
}
```

---

## 9. Rate Limiting

IP-based rate limiting uses **Bucket4j** with Caffeine-backed buckets:

```java
@Service
public class RateLimiterService {

    private static final int MAX_REQUESTS = 5;
    private static final int WINDOW_MINUTES = 1;

    public boolean isRateLimited(String ip) {
        if (ip == null || BYPASS_IPS.contains(ip)) {
            return false;
        }
        Bucket bucket = getBuckets().get(ip, _ -> buildBucket());
        return !bucket.tryConsume(1);
    }

    private Bucket buildBucket() {
        return Bucket.builder()
            .addLimit(Bandwidth.builder()
                .capacity(MAX_REQUESTS)
                .refillGreedy(MAX_REQUESTS, Duration.ofMinutes(WINDOW_MINUTES))
                .build())
            .build();
    }
}
```

---

## 10. Post-Evaluation Consequences

When a transaction is blocked or flagged, Verveguard can take automatic actions:

```java
@Async  // Non-blocking
@Transactional(propagation = Propagation.REQUIRES_NEW)  // Isolated transaction
public void applyConsequences(FraudEvaluationContext ctx, String cardHash,
                               FraudStatus status, List<String> flags) {
    if (status == FraudStatus.BLOCKED) {
        blockCard(cardHash);           // Disable the card
        simulateFraudAlert(ctx, ...);  // Send alert to merchant
    } else if (status == FraudStatus.SUSPICIOUS) {
        simulateFraudAlert(ctx, ...);  // Alert only, no block
    }
}
```

---

## 11. Observability

### AOP-Based Performance Logging

```java
@Aspect
@Component
public class ObservabilityAspect {

    @Around("@annotation(Observed) || @within(Observed)")
    public Object observe(ProceedingJoinPoint pjp) throws Throwable {
        String className = pjp.getTarget().getClass().getSimpleName();
        String method = pjp.getSignature().getName();

        log.info("→ {}.{}", className, method);
        long start = System.currentTimeMillis();

        try {
            Object result = pjp.proceed();
            log.info("← {}.{} completed in {}ms", className, method,
                     System.currentTimeMillis() - start);
            return result;
        } catch (Exception ex) {
            log.error("← {}.{} failed in {}ms — {}", className, method,
                      System.currentTimeMillis() - start, ex.getMessage());
            throw ex;
        }
    }
}
```

### Custom Health Indicator

```java
@Component
public class CacheHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        CacheStats stats = blacklistedMerchantCache.stats();

        details.put("merchantBlacklist.hitRate", stats.hitRate() * 100 + "%");
        details.put("merchantBlacklist.hitCount", stats.hitCount());
        details.put("merchantBlacklist.missCount", stats.missCount());

        boolean healthy = stats.hitRate() >= 0.5 || stats.requestCount() == 0;

        return healthy ? Health.up().withDetails(details).build()
                       : Health.down().withDetails(details).build();
    }
}
```

---

## 12. Challenges & Solutions

### Challenge 1: Cold Cache Stampede

**Problem**: When the app starts, all caches are empty. The first burst of requests all hit the database simultaneously.

**Solution**:
- Per-key locking in `TieredCache`
- JVM warmup phase before accepting traffic
- Prefetch common data on startup

### Challenge 2: Cross-Cutting Data Access

**Problem**: Gates need data from multiple sources (DB, Redis, external APIs). Passing dependencies to each gate creates coupling.

**Solution**: The `FraudDataProvider` abstraction. Gates call `data.isBlacklisted()` without knowing if it comes from cache, DB, or an API.

### Challenge 3: Velocity Accuracy vs Latency

**Problem**: Accurate velocity counts require querying all recent transactions. But DB queries add latency.

**Solution**: Redis sorted sets with timestamp-based scoring. Range queries are O(log N + M) where M is the result count — fast even with millions of entries.

### Challenge 4: Thread Safety in Prefetch

**Problem**: Request data is prefetched once and used by multiple gates. Without care, data could leak between requests.

**Solution**: `ThreadLocal<FraudDataSnapshot>` ensures each request thread has its own isolated snapshot. `clear()` in `finally` block prevents memory leaks.

### Challenge 5: Database Writes Under Load

**Problem**: Logging every fraud attempt to the database creates write pressure.

**Solution**:
- JDBC with stored procedures (no ORM overhead)
- Async consequence handling (`@Async`)
- Batch writes for high-volume scenarios

---

## 13. Testing Strategy

### Load Testing

```bash
# Stress test — full fraud evaluation under load
k6 run stress.js -e VUS=200 -e DURATION=2m

# Overhead test — pure framework cost via /ping
k6 run overhead.js -e VUS=200 -e THRESHOLD=100

# Cache benchmark — measure cache speedup
k6 run cache-benchmark.js
```

### Key Metrics to Monitor

| Metric | Target |
|--------|--------|
| p95 latency | <100ms |
| Cache hit rate | >90% |
| Error rate | <1% |

---

## 14. API Reference

### Evaluate Transaction

```http
POST /api/v1/fraud/evaluate
Authorization: Bearer {token}
Content-Type: application/json

{
    "amount": 50000.00,
    "currency": "NGN",
    "cardNumber": "4111111111111111"
}
```

**Response**:
```json
{
    "status": "success",
    "data": "CLEAN"
}
```

Possible values: `CLEAN`, `SUSPICIOUS`, `BLOCKED`

### Get Fraud Attempts (Admin)

```http
GET /api/v1/fraud/attempts?page=1&size=10
Authorization: Bearer {admin_token}
```

---

## 15. Deployment Considerations

### Required Infrastructure

- **PostgreSQL**: Main data store
- **Redis**: Velocity tracking, L2 cache
- **GeoIP Database**: MaxMind GeoLite2-City.mmdb (optional, for location anomaly)

### Environment Variables

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/verveguard
SPRING_REDIS_HOST=localhost
GEOIP_DATABASE_PATH=/opt/geoip/GeoLite2-City.mmdb
```

### JVM Tuning

```bash
# Recommended for low-latency
-XX:+UseZGC
-XX:+ZGenerational
-Xms512m
-Xmx512m
```

---

## 16. Summary

Verveguard achieves real-time fraud detection through:

1. **Fail-fast pipeline**: Hard blocks exit immediately
2. **Score accumulation**: Soft flags sum to a decision
3. **Prefetched data**: One query fetches all static data
4. **Tiered caching**: L1 (Caffeine) + L2 (Redis) with stampede protection
5. **Redis velocity**: Sub-millisecond transaction counting
6. **Async consequences**: Non-blocking post-evaluation actions
7. **Full observability**: AOP logging, health indicators, Prometheus metrics
