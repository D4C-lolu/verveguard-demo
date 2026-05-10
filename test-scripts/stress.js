// ── VERVEGUARD PERFORMANCE OBSERVATION SCRIPT ─────────────────────────────────
//
//   BASE_URL=http://myhost:8080/api/v1 VUS=200 DURATION=1m k6 run stress.js
//   EXECUTOR=ramp MAX_VUS=200 RAMP_DURATION=30s SUSTAIN=1m k6 run stress.js
//   EXECUTOR=iterations VUS=50 ITERATIONS=1000 k6 run stress.js
//   SCENARIO=admin VUS=100 DURATION=2m k6 run stress.js
// ─────────────────────────────────────────────────────────────────────────────

import http from 'k6/http';
import { check, sleep } from 'k6';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';
import { Trend, Rate, Counter } from 'k6/metrics';

// ── ENV CONFIG ────────────────────────────────────────────────────────────────
const BASE_URL      = __ENV.BASE_URL      || 'http://localhost:8080/api/v1';
const SCENARIO      = __ENV.SCENARIO      || 'merchant';
const EXECUTOR      = __ENV.EXECUTOR      || 'constant';
const PASSWORD      = __ENV.PASSWORD      || 'Admin123!';
const NUM_MERCHANTS = parseInt(__ENV.NUM_MERCHANTS || '20');
const THINK_MS      = parseFloat(__ENV.THINK_MS    || '300');

const VUS           = parseInt(__ENV.VUS           || '200');
const DURATION      = __ENV.DURATION      || '1m';
const MAX_VUS       = parseInt(__ENV.MAX_VUS       || VUS);
const RAMP_DURATION = __ENV.RAMP_DURATION || '30s';
const SUSTAIN       = __ENV.SUSTAIN       || DURATION;
const ITERATIONS    = parseInt(__ENV.ITERATIONS    || '1000');

// ── WARMUP CONFIG ─────────────────────────────────────────────────────────────
const WARMUP_RAMP    = '15s';
const WARMUP_SUSTAIN = '15s';
const WARMUP_TOTAL_S = 35; // ramp + sustain + 5s rampdown buffer

// ── IP POOL ───────────────────────────────────────────────────────────────────
const PUBLIC_IPS = [
    '8.8.8.8',
    '1.1.1.1',
    '9.9.9.9',
    '208.67.222.222',
    '77.88.8.8',
    '185.228.168.9',
];

// ── METRICS ───────────────────────────────────────────────────────────────────
const evalDuration = new Trend('evaluate_duration', true);
const evalFailRate = new Rate('evaluate_fail_rate');
const evalCount    = new Counter('evaluate_count');

// ── OPTIONS ───────────────────────────────────────────────────────────────────
function buildMeasureScenario() {
    switch (EXECUTOR) {
        case 'ramp':
            return {
                executor: 'ramping-vus',
                startVUs: 0,
                startTime: `${WARMUP_TOTAL_S}s`,
                stages: [
                    { duration: RAMP_DURATION, target: MAX_VUS },
                    { duration: SUSTAIN,       target: MAX_VUS },
                    { duration: '15s',         target: 0 },
                ],
                exec: 'measureFn',
            };
        case 'iterations':
            return {
                executor:    'shared-iterations',
                vus:         VUS,
                iterations:  ITERATIONS,
                maxDuration: '5m',
                startTime:   `${WARMUP_TOTAL_S}s`,
                exec:        'measureFn',
            };
        default:
            return {
                executor:  'constant-vus',
                vus:       VUS,
                duration:  DURATION,
                startTime: `${WARMUP_TOTAL_S}s`,
                exec:      'measureFn',
            };
    }
}

export const options = {
    scenarios: {
        warmup: {
            executor:  'ramping-vus',
            startVUs:  0,
            stages: [
                { duration: WARMUP_RAMP,    target: VUS },
                { duration: WARMUP_SUSTAIN, target: VUS },
                { duration: '5s',           target: 0   },
            ],
            gracefulRampDown: '5s',
            exec: 'warmupFn',
        },
        verveguard_test: buildMeasureScenario(),
    },
    // no thresholds — pure observation
};

// ── SETUP ─────────────────────────────────────────────────────────────────────
export function setup() {
    return SCENARIO === 'admin' ? setupAdmin() : setupMerchants();
}

function setupAdmin() {
    const res = http.post(
        `${BASE_URL}/auth/login`,
        JSON.stringify({ email: 'cleanuser@verveguard.com', password: PASSWORD }),
        { headers: { 'Content-Type': 'application/json' } }
    );
    const merchants = Array.from({ length: NUM_MERCHANTS }, (_, i) => ({
        id: i + 1,
        cardNumber: `4${String(i + 1).padStart(15, '0')}`,
    }));
    return { merchants, adminToken: res.json('data.accessToken') };
}

function setupMerchants() {
    const merchants = [];
    for (let i = 1; i <= NUM_MERCHANTS; i++) {
        const res = http.post(
            `${BASE_URL}/auth/login`,
            JSON.stringify({ email: `merchant${i}@stresstest.com`, password: PASSWORD }),
            { headers: { 'Content-Type': 'application/json' } }
        );
        if (res.status === 200) {
            merchants.push({
                id:         i,
                token:      res.json('data.accessToken'),
                cardNumber: `4${String(i).padStart(15, '0')}`,
            });
        }
    }
    return { merchants };
}

// ── WARMUP (hits evaluate endpoint to warm JVM, pools, caches) ────────────────
export function warmupFn(data) {
    if (!data.merchants?.length) return;

    const merchant = data.merchants[__VU % data.merchants.length];
    const token    = SCENARIO === 'admin' ? data.adminToken : merchant.token;

    const headers = {
        'Content-Type':    'application/json',
        'Authorization':   `Bearer ${token}`,
        'X-Forwarded-For': PUBLIC_IPS[__VU % PUBLIC_IPS.length],
    };

    const payload = JSON.stringify({
        amount:     100 + Math.floor(Math.random() * 9900),
        currency:   'NGN',
        cardNumber: merchant.cardNumber,
    });

    const evalUrl = SCENARIO === 'admin'
        ? `${BASE_URL}/fraud/evaluate/${merchant.id}`
        : `${BASE_URL}/fraud/evaluate`;

    http.post(evalUrl, payload, { headers, tags: { name: 'warmup' } });
    sleep(THINK_MS / 1000);
}

// ── MEASURE ───────────────────────────────────────────────────────────────────
export function measureFn(data) {
    if (!data.merchants?.length) return;

    const merchant = data.merchants[__VU % data.merchants.length];
    const token    = SCENARIO === 'admin' ? data.adminToken : merchant.token;

    const headers = {
        'Content-Type':    'application/json',
        'Authorization':   `Bearer ${token}`,
        'X-Forwarded-For': PUBLIC_IPS[__VU % PUBLIC_IPS.length],
    };

    const payload = JSON.stringify({
        amount:     100 + Math.floor(Math.random() * 9900),
        currency:   'NGN',
        cardNumber: merchant.cardNumber,
    });

    const evalUrl = SCENARIO === 'admin'
        ? `${BASE_URL}/fraud/evaluate/${merchant.id}`
        : `${BASE_URL}/fraud/evaluate`;

    const res = http.post(evalUrl, payload, { headers, tags: { name: 'evaluate' } });

    evalDuration.add(res.timings.duration);
    evalFailRate.add(res.status !== 200);
    evalCount.add(1);
    check(res, { 'status 200': r => r.status === 200 });
    sleep(THINK_MS / 1000);
}

// ── SUMMARY ───────────────────────────────────────────────────────────────────
export function handleSummary(data) {
    const ev    = data.metrics.evaluate_duration?.values ?? {};
    const ms    = (key) => ev[key] != null ? Number(ev[key]).toFixed(0) : null;
    const rps   = data.metrics.http_reqs?.values?.rate ?? 0;
    const fail  = (data.metrics.evaluate_fail_rate?.values?.rate ?? 0) * 100;
    const total = data.metrics.evaluate_count?.values?.count
        ?? data.metrics.http_reqs?.values?.count
        ?? 0;

    const p50  = ms('p(50)');
    const p90  = ms('p(90)');
    const p95  = ms('p(95)');
    const p99  = ms('p(99)');
    const pMax = ms('max');

    const modeLabel = EXECUTOR === 'ramp'
        ? `RAMP 0→${MAX_VUS} VUs / sustain ${SUSTAIN}`
        : EXECUTOR === 'iterations'
            ? `${VUS} VUs / ${ITERATIONS} iterations`
            : `CONSTANT ${VUS} VUs / ${DURATION}`;

    function interpretLatency(p95ms) {
        if (p95ms === null) return 'No latency data recorded.';
        const v = Number(p95ms);
        if (v < 200)  return `Latency looks excellent. 95% of requests completed in under ${v}ms — the service is responding very quickly under this load.`;
        if (v < 500)  return `Latency is good. The p95 of ${v}ms is within a comfortable range for a fraud evaluation service.`;
        if (v < 1000) return `Latency is acceptable but worth watching. p95 hit ${v}ms — fine for now, but leaves little headroom before user-facing impact.`;
        if (v < 2000) return `Latency is degraded. p95 reached ${v}ms, which is likely noticeable to users or downstream systems. Investigate DB query times and connection pool saturation.`;
        return `Latency is high. p95 at ${v}ms suggests the service is struggling under this load. Check for slow queries, lock contention, or resource exhaustion.`;
    }

    function interpretErrors(failPct) {
        if (failPct === 0)    return 'No errors recorded — the service handled all requests successfully.';
        if (failPct < 1)      return `Error rate is very low at ${failPct.toFixed(2)}% — likely transient issues, nothing systemic.`;
        if (failPct < 5)      return `Error rate of ${failPct.toFixed(2)}% is noticeable. Check logs for patterns — could be timeouts, auth issues, or intermittent 5xx responses.`;
        if (failPct < 20)     return `Error rate of ${failPct.toFixed(2)}% is significant. The service is failing a meaningful share of requests — likely under resource pressure.`;
        return `Error rate of ${failPct.toFixed(2)}% is critical. The service is failing most requests and is likely overwhelmed or misconfigured for this load.`;
    }

    function interpretThroughput(rps, failPct) {
        const effective = rps * (1 - failPct / 100);
        return `The service processed ${rps.toFixed(2)} req/s total, with roughly ${effective.toFixed(2)} req/s succeeding.`;
    }

    console.log(`
==================================================================
VERVEGUARD PERFORMANCE REPORT — ${modeLabel}  [${SCENARIO.toUpperCase()}]
==================================================================
LATENCY (ms)
  p50  : ${p50  ?? 'N/A'}
  p90  : ${p90  ?? 'N/A'}
  p95  : ${p95  ?? 'N/A'}
  p99  : ${p99  ?? 'N/A'}
  max  : ${pMax ?? 'N/A'}

THROUGHPUT
  RPS  : ${rps.toFixed(2)} req/s
  total: ${total}

ERRORS
  fail : ${fail.toFixed(2)}%

──────────────────────────────────────────────────────────────────
OBSERVATIONS
  ${interpretLatency(p95)}
  ${interpretThroughput(rps, fail)}
  ${interpretErrors(fail)}
==================================================================`);

    return {
        stdout: textSummary(data, { indent: '  ', enableColors: false }),
    };
}