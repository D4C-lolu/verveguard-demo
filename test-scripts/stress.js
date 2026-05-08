import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

/**
 * VerveguardAPI Stress Test
 *
 * Pre-authenticates merchants in setup(), then stress tests fraud evaluation.
 * Also measures infrastructure overhead via a ping endpoint.
 *
 * Scenarios:
 *   - merchant: POST /fraud/evaluate (default)
 *   - admin:    POST /fraud/evaluate/{merchantId}
 *
 * Usage:
 *   k6 run stress.js
 *   k6 run -e SCENARIO=admin stress.js
 *   k6 run -e BASE_URL=http://host:8080/api/v1 stress.js
 */

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api/v1';
const SCENARIO = __ENV.SCENARIO || 'merchant';
const PASSWORD = 'Admin123!';
const NUM_MERCHANTS = 20;

// Metrics - Evaluate
const evaluateDuration = new Trend('evaluate_duration', true);
const evaluateFailRate = new Rate('evaluate_fail_rate');
const evaluateCount = new Counter('evaluate_count');

// Metrics - Ping (overhead baseline)
const pingDuration = new Trend('ping_duration', true);
const pingFailRate = new Rate('ping_fail_rate');

export const options = {
    scenarios: {
        load: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '10s', target: 15 },
                { duration: '30s', target: 30 },
                { duration: '10s', target: 0 },
            ],
            gracefulRampDown: '10s',
        },
    },
    thresholds: {
        'evaluate_duration': ['p(95)<1000'],
        'evaluate_fail_rate': ['rate<0.05'],
        'ping_fail_rate': ['rate<0.05'],
    },
};

// -----------------------------------------------------------------------------
// Setup: Run ONCE before all VUs - authenticate all merchants
// -----------------------------------------------------------------------------

export function setup() {
    if (SCENARIO === 'admin') {
        // Admin scenario: only authenticate admin, use merchant data without tokens
        console.log(`\n========== Setup: Admin scenario ==========\n`);

        const merchants = [];
        for (let i = 1; i <= NUM_MERCHANTS; i++) {
            merchants.push({
                id: i,
                email: `merchant${i}@stresstest.com`,
                cardNumber: `4${String(i).padStart(15, '0')}`,
            });
        }

        const adminEmail = 'cleanuser@verveguard.com';
        const res = http.post(
            `${BASE_URL}/auth/login`,
            JSON.stringify({ email: adminEmail, password: PASSWORD }),
            {
                headers: { 'Content-Type': 'application/json' },
                timeout: '60s',
            }
        );

        if (res.status === 200) {
            const adminToken = res.json('data.accessToken');
            console.log(`  [OK] ${adminEmail} (admin)`);
            console.log(`\n========== Setup complete: admin ready, ${merchants.length} merchant targets ==========\n`);
            return { merchants, adminToken };
        } else {
            console.log(`  [FAIL] ${adminEmail} - ${res.status}`);
            return { merchants };
        }
    }

    // Merchant scenario: authenticate all merchants
    console.log(`\n========== Setup: Authenticating ${NUM_MERCHANTS} merchants ==========\n`);

    const merchants = [];

    for (let i = 1; i <= NUM_MERCHANTS; i++) {
        const email = `merchant${i}@stresstest.com`;
        const cardNumber = `4${String(i).padStart(15, '0')}`;

        const res = http.post(
            `${BASE_URL}/auth/login`,
            JSON.stringify({ email, password: PASSWORD }),
            {
                headers: { 'Content-Type': 'application/json' },
                timeout: '60s',
            }
        );

        if (res.status === 200) {
            try {
                const token = res.json('data.accessToken');
                merchants.push({ id: i, email, cardNumber, token });
                console.log(`  [OK] ${email}`);
            } catch (e) {
                console.log(`  [FAIL] ${email} - parse error`);
            }
        } else {
            console.log(`  [FAIL] ${email} - ${res.status}`);
        }

        sleep(0.5);
    }

    console.log(`\n========== Setup complete: ${merchants.length} merchants ready ==========\n`);
    return { merchants };
}

// -----------------------------------------------------------------------------
// Main
// -----------------------------------------------------------------------------

export default function (data) {
    if (!data.merchants || data.merchants.length === 0) {
        console.error('No merchants available - setup failed');
        sleep(1);
        return;
    }

    if (SCENARIO === 'admin') {
        runAdminScenario(data);
    } else {
        runMerchantScenario(data);
    }
}

function runMerchantScenario(data) {
    const merchant = data.merchants[__VU % data.merchants.length];

    const headers = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${merchant.token}`,
        'X-Forwarded-For': `192.168.${__VU % 256}.${__ITER % 256}`,
    };

    const payload = JSON.stringify({
        amount: 100 + Math.floor(Math.random() * 9900),
        currency: 'NGN',
        cardNumber: merchant.cardNumber,
    });

    // ── 1. Ping: measure pure infrastructure overhead ──────────────────────
    const pingRes = http.get(`${BASE_URL.replace('/api/v1', '')}/actuator/health/ping`, {
        headers,
        tags: { name: 'ping' },
        timeout: '10s',
    });

    pingDuration.add(pingRes.timings.duration);

    const pingOk = check(pingRes, {
        'ping status 200': (r) => r.status === 200,
    });
    pingFailRate.add(!pingOk);

    // ── 2. Evaluate: the real fraud endpoint ───────────────────────────────
    const evalRes = http.post(`${BASE_URL}/fraud/evaluate`, payload, {
        headers,
        tags: { name: 'fraud-evaluate' },
        timeout: '30s',
    });

    evaluateDuration.add(evalRes.timings.duration);
    evaluateCount.add(1);

    const evalOk = check(evalRes, {
        'evaluate status 200': (r) => r.status === 200,
    });
    evaluateFailRate.add(!evalOk);

    if (!evalOk) {
        console.error(`FAIL: ${merchant.email} status=${evalRes.status}`);
    }

    sleep(0.2 + Math.random() * 0.1);
}

function runAdminScenario(data) {
    if (!data.adminToken) {
        console.error('No admin token - setup failed');
        sleep(1);
        return;
    }

    const merchant = data.merchants[Math.floor(Math.random() * data.merchants.length)];

    const headers = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${data.adminToken}`,
        'X-Forwarded-For': `10.0.${__VU % 256}.${__ITER % 256}`,
    };

    const payload = JSON.stringify({
        amount: 100 + Math.floor(Math.random() * 9900),
        currency: 'NGN',
        cardNumber: merchant.cardNumber,
    });

    // ── 1. Ping overhead ───────────────────────────────────────────────────
    const pingRes = http.get(`${BASE_URL.replace('/api/v1', '')}/actuator/health/ping`, {
        headers,
        tags: { name: 'ping' },
        timeout: '10s',
    });

    pingDuration.add(pingRes.timings.duration);
    const pingOk = check(pingRes, { 'ping status 200': (r) => r.status === 200 });
    pingFailRate.add(!pingOk);

    // ── 2. Admin evaluate ──────────────────────────────────────────────────
    const evalRes = http.post(`${BASE_URL}/fraud/evaluate/${merchant.id}`, payload, {
        headers,
        tags: { name: 'fraud-evaluate-admin' },
        timeout: '30s',
    });

    evaluateDuration.add(evalRes.timings.duration);
    evaluateCount.add(1);

    const evalOk = check(evalRes, { 'evaluate status 200': (r) => r.status === 200 });
    evaluateFailRate.add(!evalOk);

    if (!evalOk) {
        console.error(`ADMIN FAIL: merchantId=${merchant.id} status=${evalRes.status}`);
    }

    sleep(0.2 + Math.random() * 0.1);
}

// -----------------------------------------------------------------------------
// Summary
// -----------------------------------------------------------------------------

function fmt(val, unit, width = 0) {
    const str = val != null ? `${val.toFixed(2)} ${unit}` : 'N/A';
    return width > 0 ? str.padEnd(width) : str;
}

function grade(p95) {
    if (p95 == null) return '?';
    if (p95 < 100)  return 'Excellent';
    if (p95 < 300)  return 'Good';
    if (p95 < 600)  return 'Acceptable';
    if (p95 < 1000) return 'Slow';
    return 'Critical';
}

export function handleSummary(data) {
    const scenario = SCENARIO === 'admin' ? 'ADMIN' : 'MERCHANT';

    const ev  = data.metrics.evaluate_duration?.values;
    const pi  = data.metrics.ping_duration?.values;
    const efr = data.metrics.evaluate_fail_rate?.values;
    const pfr = data.metrics.ping_fail_rate?.values;
    const ec  = data.metrics.evaluate_count?.values;
    const req = data.metrics.http_reqs?.values;
    const its = data.metrics.iterations?.values;

    // Derived: app logic cost = evaluate - ping
    const appLogicAvg = (ev?.avg != null && pi?.avg != null) ? ev.avg - pi.avg : null;
    const appLogicP95 = (ev?.['p(95)'] != null && pi?.['p(95)'] != null) ? ev['p(95)'] - pi['p(95)'] : null;

    // Column widths
    const COL1 = 10;  // Metric
    const COL2 = 18;  // Ping
    const COL3 = 18;  // Evaluate
    const COL4 = 14;  // App Logic
    const W = COL1 + COL2 + COL3 + COL4 + 6;
    const line  = '-'.repeat(W);
    const dline = '='.repeat(W);

    console.log('\n' + dline);
    console.log(`  ${scenario} STRESS TEST RESULTS`);
    console.log(dline);
    console.log(`  Endpoint : ${SCENARIO === 'admin' ? 'POST /fraud/evaluate/{merchantId}' : 'POST /fraud/evaluate'}`);
    console.log(`  Base URL : ${BASE_URL}`);

    // ── Latency ─────────────────────────────────────────────────────────────
    console.log('\n  LATENCY BREAKDOWN');
    console.log('  ' + line);
    console.log(`  ${'Metric'.padEnd(COL1)} ${'Ping'.padEnd(COL2)} ${'Evaluate'.padEnd(COL3)} ${'App Logic'.padEnd(COL4)}`);
    console.log('  ' + line);

    const rows = [
        ['Min',    pi?.min,        ev?.min],
        ['Avg',    pi?.avg,        ev?.avg],
        ['Median', pi?.med,        ev?.med],
        ['p(90)',  pi?.['p(90)'],  ev?.['p(90)']],
        ['p(95)',  pi?.['p(95)'],  ev?.['p(95)']],
        ['Max',    pi?.max,        ev?.max],
    ];

    for (const [label, pingVal, evalVal] of rows) {
        const logic = (pingVal != null && evalVal != null) ? evalVal - pingVal : null;
        console.log(
            `  ${label.padEnd(COL1)} ${fmt(pingVal, 'ms', COL2)} ${fmt(evalVal, 'ms', COL3)} ${fmt(logic, 'ms', COL4)}`
        );
    }

    // ── Overhead summary ────────────────────────────────────────────────────
    console.log('\n  OVERHEAD ANALYSIS');
    console.log('  ' + line);
    if (pi?.avg != null && ev?.avg != null) {
        const overheadPct = ((pi.avg / ev.avg) * 100).toFixed(1);
        const logicPct    = (100 - overheadPct).toFixed(1);
        console.log(`  Avg response breakdown:`);
        console.log(`    Infrastructure : ${fmt(pi.avg, 'ms', 14)} (${overheadPct}%)`);
        console.log(`    App logic      : ${fmt(appLogicAvg, 'ms', 14)} (${logicPct}%)`);
        console.log(`    Total          : ${fmt(ev.avg, 'ms')}`);
    }
    if (appLogicP95 != null) {
        console.log(`  p(95) app logic  : ${fmt(appLogicP95, 'ms')}`);
    }

    // ── Performance grade ───────────────────────────────────────────────────
    console.log('\n  PERFORMANCE GRADE');
    console.log('  ' + line);
    console.log(`  Evaluate p(95)   : ${fmt(ev?.['p(95)'], 'ms', 14)} -> ${grade(ev?.['p(95)'])}`);
    console.log(`  Ping p(95)       : ${fmt(pi?.['p(95)'], 'ms', 14)} -> ${grade(pi?.['p(95)'])}`);
    console.log(`  Threshold        : p(95) < 1000ms`);

    // ── Throughput ──────────────────────────────────────────────────────────
    console.log('\n  THROUGHPUT');
    console.log('  ' + line);
    if (req?.count != null) console.log(`  HTTP requests    : ${req.count}`);
    if (req?.rate  != null) console.log(`  Requests/sec     : ${req.rate.toFixed(2)}`);
    if (ec?.count  != null) console.log(`  Evaluations      : ${ec.count}`);
    if (its?.count != null) console.log(`  Iterations       : ${its.count}`);

    // ── Reliability ─────────────────────────────────────────────────────────
    console.log('\n  RELIABILITY');
    console.log('  ' + line);
    if (efr?.rate != null) {
        const s = (100 - efr.rate * 100).toFixed(2);
        const f = (efr.rate * 100).toFixed(2);
        console.log(`  Evaluate         : ${s}% success, ${f}% fail`);
    }
    if (pfr?.rate != null) {
        const s = (100 - pfr.rate * 100).toFixed(2);
        const f = (pfr.rate * 100).toFixed(2);
        console.log(`  Ping             : ${s}% success, ${f}% fail`);
    }

    console.log('\n' + dline + '\n');

    return {};
}