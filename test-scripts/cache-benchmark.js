// ── VERVEGUARD CACHE BENCHMARK ────────────────────────────────────────────────
//
// Measures blacklist cache speedup by comparing cold (miss) vs warm (hit) latency.
// Restarts the app between phases for a true cold start, or use the provided
// methodology to manually restart.
//
// Run:
//   k6 run cache-benchmark.js
//   k6 run cache-benchmark.js -e BASE_URL=http://app:8080/api/v1
// ─────────────────────────────────────────────────────────────────────────────

import http from 'k6/http';
import { check, sleep } from 'k6';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';
import { Counter, Trend } from 'k6/metrics';

// ── CONFIG ────────────────────────────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api/v1';
const EMAIL = __ENV.EMAIL || 'testmerchant@verveguard.com';
const PASSWORD = __ENV.PASSWORD || 'Admin123!';
const CARD_NUMBER = '4111111111111111'; // testmerchant's card from seed data
const ACTUATOR_URL = __ENV.ACTUATOR_URL || 'http://localhost:8080/actuator';

// ── METRICS ───────────────────────────────────────────────────────────────────
const coldLatency = new Trend('cold_latency_ms', true);
const warmLatency = new Trend('warm_latency_ms', true);
const coldCount = new Counter('cold_requests');
const warmCount = new Counter('warm_requests');

// ── OPTIONS ───────────────────────────────────────────────────────────────────
export const options = {
    scenarios: {
        // Phase 1: Single cold request (first hit after cache is empty)
        cold: {
            executor: 'shared-iterations',
            vus: 1,
            iterations: 1,
            exec: 'coldRequest',
            startTime: '0s',
        },
        // Phase 2: Warm requests (cache is populated)
        warm: {
            executor: 'constant-vus',
            vus: 10,
            duration: '15s',
            exec: 'warmRequest',
            startTime: '2s', // Start after cold request completes
        },
        // Phase 3: Fetch cache stats from actuator
        stats: {
            executor: 'shared-iterations',
            vus: 1,
            iterations: 1,
            exec: 'fetchCacheStats',
            startTime: '20s',
        },
    },
    thresholds: {
        warm_latency_ms: ['p(95)<50'], // Warm requests should be fast
    },
};

// ── SETUP ─────────────────────────────────────────────────────────────────────
export function setup() {
    // Login as merchant to get token
    const loginRes = http.post(
        `${BASE_URL}/auth/login`,
        JSON.stringify({ email: EMAIL, password: PASSWORD }),
        { headers: { 'Content-Type': 'application/json' } }
    );

    if (loginRes.status !== 200) {
        throw new Error(`Login failed: ${loginRes.status} ${loginRes.body}`);
    }

    const token = loginRes.json('data.accessToken');
    if (!token) {
        throw new Error(`No token in response: ${loginRes.body}`);
    }

    console.log(`✓ Logged in as ${EMAIL}`);
    console.log(`✓ Testing with card: ${CARD_NUMBER}`);

    return { token };
}

// ── COLD REQUEST (cache miss) ─────────────────────────────────────────────────
export function coldRequest(data) {
    const payload = JSON.stringify({
        amount: 1000.00,
        currency: 'NGN',
        cardNumber: CARD_NUMBER,
    });

    const res = http.post(`${BASE_URL}/fraud/evaluate`, payload, {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${data.token}`,
        },
        tags: { name: 'cold' },
    });

    coldLatency.add(res.timings.duration);
    coldCount.add(1);

    check(res, {
        'cold request status 200': (r) => r.status === 200,
    });

    console.log(`COLD REQUEST: ${res.timings.duration.toFixed(2)}ms`);
}

// ── WARM REQUEST (cache hit) ──────────────────────────────────────────────────
export function warmRequest(data) {
    const payload = JSON.stringify({
        amount: 1000.00,
        currency: 'NGN',
        cardNumber: CARD_NUMBER,
    });

    const res = http.post(`${BASE_URL}/fraud/evaluate`, payload, {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${data.token}`,
        },
        tags: { name: 'warm' },
    });

    warmLatency.add(res.timings.duration);
    warmCount.add(1);

    check(res, {
        'warm request status 200': (r) => r.status === 200,
    });

    sleep(0.1); // Small delay between requests
}

// ── FETCH CACHE STATS ─────────────────────────────────────────────────────────
export function fetchCacheStats() {
    const res = http.get(`${ACTUATOR_URL}/health`, {
        tags: { name: 'actuator' },
    });

    if (res.status === 200) {
        try {
            const health = res.json();
            const cacheDetails = health.components?.cache?.details || {};

            console.log('\n═══════════════════════════════════════════════════════');
            console.log('CACHE HEALTH INDICATOR STATS');
            console.log('═══════════════════════════════════════════════════════');

            // Merchant blacklist cache
            if (cacheDetails['merchantBlacklist.hitRate']) {
                console.log(`Merchant Blacklist Cache:`);
                console.log(`  Hit Rate:  ${cacheDetails['merchantBlacklist.hitRate']}`);
                console.log(`  Hit Count: ${cacheDetails['merchantBlacklist.hitCount']}`);
                console.log(`  Miss Count: ${cacheDetails['merchantBlacklist.missCount']}`);
                console.log(`  Size: ${cacheDetails['merchantBlacklist.size']}`);
            }

            // Fraud eval cache
            if (cacheDetails['fraudEval.hitRate']) {
                console.log(`\nFraud Eval Cache:`);
                console.log(`  Hit Rate:  ${cacheDetails['fraudEval.hitRate']}`);
                console.log(`  Hit Count: ${cacheDetails['fraudEval.hitCount']}`);
                console.log(`  Miss Count: ${cacheDetails['fraudEval.missCount']}`);
            }

            console.log('═══════════════════════════════════════════════════════\n');
        } catch (e) {
            console.log('Could not parse health response:', e.message);
        }
    } else {
        console.log(`Actuator health check failed: ${res.status}`);
    }
}

// ── SUMMARY ───────────────────────────────────────────────────────────────────
export function handleSummary(data) {
    const cold = data.metrics.cold_latency_ms?.values ?? {};
    const warm = data.metrics.warm_latency_ms?.values ?? {};

    const coldP50 = cold['p(50)'] ?? 0;
    const coldP95 = cold['p(95)'] ?? 0;
    const warmP50 = warm['p(50)'] ?? 0;
    const warmP95 = warm['p(95)'] ?? 0;

    const speedupP50 = warmP50 > 0 ? (coldP50 / warmP50).toFixed(1) : 'N/A';
    const speedupP95 = warmP95 > 0 ? (coldP95 / warmP95).toFixed(1) : 'N/A';

    const coldTotal = data.metrics.cold_requests?.values?.count ?? 0;
    const warmTotal = data.metrics.warm_requests?.values?.count ?? 0;

    console.log(`
══════════════════════════════════════════════════════════════════════
VERVEGUARD CACHE BENCHMARK RESULTS
══════════════════════════════════════════════════════════════════════

LATENCY COMPARISON

              │  Cold (miss)  │  Warm (hit)   │  Speedup
──────────────┼───────────────┼───────────────┼──────────
  p50         │  ${coldP50.toFixed(2).padStart(8)} ms │  ${warmP50.toFixed(2).padStart(8)} ms │  ${String(speedupP50).padStart(5)}x
  p95         │  ${coldP95.toFixed(2).padStart(8)} ms │  ${warmP95.toFixed(2).padStart(8)} ms │  ${String(speedupP95).padStart(5)}x

REQUESTS
  Cold (cache miss):  ${coldTotal}
  Warm (cache hit):   ${warmTotal}

NOTE: For accurate cold measurements, restart the app before running.
      The warm speedup demonstrates cache effectiveness under load.

══════════════════════════════════════════════════════════════════════`);

    return {
        stdout: textSummary(data, { indent: '  ', enableColors: false }),
    };
}
