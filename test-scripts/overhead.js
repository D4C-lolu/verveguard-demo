// ── VERVEGUARD OVERHEAD TEST ──────────────────────────────────────────────────
//
// Warms the JVM first, then measures pure framework overhead by hitting /ping.
//
// Run:
//   k6 run overhead.js
//   k6 run overhead.js -e BASE_URL=http://app:8080/api/v1 -e VUS=200 -e DURATION=30s -e THRESHOLD=100
// ─────────────────────────────────────────────────────────────────────────────

import http from 'k6/http';
import { check, sleep } from 'k6';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';
import { Counter, Rate, Trend } from 'k6/metrics';

// ── CONFIG ────────────────────────────────────────────────────────────────────
const BASE_URL   = __ENV.BASE_URL  || 'http://localhost:8080/api/v1';
const EMAIL      = __ENV.EMAIL     || 'cleanuser@verveguard.com';
const PASSWORD   = __ENV.PASSWORD  || 'Admin123!';
const VUS        = parseInt(__ENV.VUS       || '200');
const DURATION   = __ENV.DURATION  || '30s';
const THRESHOLD  = parseInt(__ENV.THRESHOLD || '100');
const THINK_MS   = parseFloat(__ENV.THINK_MS || '0');

// Warmup: ramp to full VUs over 15s, sustain for 15s — enough to fill pools and JIT
const WARMUP_RAMP    = '15s';
const WARMUP_SUSTAIN = '15s';
const WARMUP_TOTAL_S = 30; // must match ramp + sustain in seconds

// ── METRICS ───────────────────────────────────────────────────────────────────
const overheadDuration = new Trend('overhead_ms', true);
const failRate         = new Rate('fail_rate');
const reqCount         = new Counter('req_count');

// ── OPTIONS ───────────────────────────────────────────────────────────────────
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
        measure: {
            executor:  'constant-vus',
            vus:       VUS,
            duration:  DURATION,
            startTime: `${WARMUP_TOTAL_S + 5}s`, // start after warmup + rampdown buffer
            exec:      'measureFn',
        },
    },
    thresholds: {
        overhead_ms: [`p(95)<${THRESHOLD}`],
        fail_rate:   ['rate<0.01'],
    },
};

// ── SETUP ─────────────────────────────────────────────────────────────────────
export function setup() {
    const res = http.post(
        `${BASE_URL}/auth/login`,
        JSON.stringify({ email: EMAIL, password: PASSWORD }),
        { headers: { 'Content-Type': 'application/json' } }
    );

    if (res.status !== 200) {
        throw new Error(`Login failed: ${res.status} ${res.body}`);
    }

    const token = res.json('data.accessToken');
    if (!token) {
        throw new Error(`No token in response: ${res.body}`);
    }

    console.log(`✓ Logged in as ${EMAIL}`);
    return { token };
}

// ── WARMUP (metrics collected but not reported) ───────────────────────────────
export function warmupFn() {
    http.get(`${BASE_URL}/fraud/ping`, {
        tags: { name: 'warmup' },
    });
    if (THINK_MS > 0) sleep(THINK_MS / 1000);
}

// ── MEASURE ───────────────────────────────────────────────────────────────────
export function measureFn() {
    const res = http.get(
        `${BASE_URL}/fraud/ping`,
        {
            tags: { name: 'ping' },
        }
    );

    overheadDuration.add(res.timings.duration);
    failRate.add(res.status !== 200);
    reqCount.add(1);

    check(res, {
        'status 200': r => r.status === 200,
        'body ok':    r => r.body.includes('ok'),
    });

    if (THINK_MS > 0) sleep(THINK_MS / 1000);
}

// ── SUMMARY ───────────────────────────────────────────────────────────────────
export function handleSummary(data) {
    const ev    = data.metrics.overhead_ms?.values ?? {};
    const ms    = (key) => ev[key] != null ? `${Number(ev[key]).toFixed(2)} ms` : 'N/A';
    const rps   = data.metrics.http_reqs?.values?.rate ?? 0;
    const fail  = (data.metrics.fail_rate?.values?.rate ?? 0) * 100;
    const total = data.metrics.req_count?.values?.count ?? 0;
    const p95   = ev['p(95)'] != null ? Number(ev['p(95)']) : null;
    const pass  = p95 != null ? (p95 < THRESHOLD ? '✓ PASS' : '✗ FAIL') : '–';

    console.log(`
==================================================================
VERVEGUARD OVERHEAD REPORT — ${VUS} VUs / ${DURATION}  [ADMIN]
==================================================================
LATENCY  (pure framework overhead — no business logic)
  p50  : ${ms('p(50)')}
  p90  : ${ms('p(90)')}
  p95  : ${ms('p(95)')}   threshold <${THRESHOLD}ms ${pass}
  p99  : ${ms('p(99)')}
  max  : ${ms('max')}

THROUGHPUT
  RPS  : ${rps.toFixed(2)} req/s
  total: ${total}

ERRORS
  fail : ${fail.toFixed(2)}%
==================================================================`);

    return {
        stdout: textSummary(data, { indent: '  ', enableColors: false }),
    };
}