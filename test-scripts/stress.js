import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

/**
 * VerveguardAPI Stress Test
 *
 * Pre-authenticates merchants in setup(), then stress tests fraud evaluation.
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
const NUM_MERCHANTS = 20;  // Keep small for faster setup

// Metrics
const evaluateDuration = new Trend('evaluate_duration', true);
const evaluateFailRate = new Rate('evaluate_fail_rate');
const evaluateCount = new Counter('evaluate_count');

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
    },
};

// -----------------------------------------------------------------------------
// Setup: Run ONCE before all VUs - authenticate all merchants
// -----------------------------------------------------------------------------

export function setup() {
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

        sleep(0.5);  // Gentle pacing - one login per 500ms
    }

    // For admin scenario, also get admin token
    if (SCENARIO === 'admin') {
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
            return { merchants, adminToken };
        } else {
            console.log(`  [FAIL] ${adminEmail} - ${res.status}`);
        }
    }

    console.log(`\n========== Setup complete: ${merchants.length} merchants ready ==========\n`);
    return { merchants };
}

// -----------------------------------------------------------------------------
// Main: Stress test fraud evaluation with pre-authenticated tokens
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

    const payload = JSON.stringify({
        amount: 100 + Math.floor(Math.random() * 9900),
        currency: 'NGN',
        cardNumber: merchant.cardNumber,
    });

    const res = http.post(`${BASE_URL}/fraud/evaluate`, payload, {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${merchant.token}`,
            'X-Forwarded-For': `192.168.${__VU % 256}.${__ITER % 256}`,
        },
        tags: { name: 'fraud-evaluate' },
        timeout: '30s',
    });

    evaluateDuration.add(res.timings.duration);
    evaluateCount.add(1);

    const success = check(res, {
        'status 200': (r) => r.status === 200,
    });

    evaluateFailRate.add(!success);

    if (!success) {
        console.error(`FAIL: ${merchant.email} status=${res.status}`);
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

    const payload = JSON.stringify({
        amount: 100 + Math.floor(Math.random() * 9900),
        currency: 'NGN',
        cardNumber: merchant.cardNumber,
    });

    const res = http.post(`${BASE_URL}/fraud/evaluate/${merchant.id}`, payload, {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${data.adminToken}`,
            'X-Forwarded-For': `10.0.${__VU % 256}.${__ITER % 256}`,
        },
        tags: { name: 'fraud-evaluate-admin' },
        timeout: '30s',
    });

    evaluateDuration.add(res.timings.duration);
    evaluateCount.add(1);

    const success = check(res, {
        'status 200': (r) => r.status === 200,
    });

    evaluateFailRate.add(!success);

    if (!success) {
        console.error(`ADMIN FAIL: merchantId=${merchant.id} status=${res.status}`);
    }

    sleep(0.2 + Math.random() * 0.1);
}

export function handleSummary(data) {
    const scenario = SCENARIO === 'admin' ? 'Admin' : 'Merchant';

    // Extract metrics
    const evalDuration = data.metrics.evaluate_duration;
    const evalFailRate = data.metrics.evaluate_fail_rate;
    const evalCount = data.metrics.evaluate_count;
    const httpReqs = data.metrics.http_reqs;
    const iterations = data.metrics.iterations;

    console.log('\n' + '='.repeat(65));
    console.log(`  ${scenario.toUpperCase()} STRESS TEST RESULTS`);
    console.log('='.repeat(65));
    console.log(`  Endpoint:     ${SCENARIO === 'admin' ? 'POST /fraud/evaluate/{merchantId}' : 'POST /fraud/evaluate'}`);
    console.log(`  Base URL:     ${BASE_URL}`);
    console.log('-'.repeat(65));

    if (evalDuration && evalDuration.values) {
        const v = evalDuration.values;
        console.log('  LATENCY (evaluate_duration)');
        console.log(`    Min:        ${v.min?.toFixed(2) || 'N/A'} ms`);
        console.log(`    Avg:        ${v.avg?.toFixed(2) || 'N/A'} ms`);
        console.log(`    Med:        ${v.med?.toFixed(2) || 'N/A'} ms`);
        console.log(`    Max:        ${v.max?.toFixed(2) || 'N/A'} ms`);
        console.log(`    p(90):      ${v['p(90)']?.toFixed(2) || 'N/A'} ms`);
        console.log(`    p(95):      ${v['p(95)']?.toFixed(2) || 'N/A'} ms`);
        console.log(`    p(99):      ${v['p(99)']?.toFixed(2) || 'N/A'} ms`);
    }

    console.log('-'.repeat(65));
    console.log('  THROUGHPUT');
    if (httpReqs && httpReqs.values) {
        console.log(`    Total Reqs: ${httpReqs.values.count || 'N/A'}`);
        console.log(`    Req/s:      ${httpReqs.values.rate?.toFixed(2) || 'N/A'}`);
    }
    if (evalCount && evalCount.values) {
        console.log(`    Evaluations: ${evalCount.values.count || 'N/A'}`);
    }
    if (iterations && iterations.values) {
        console.log(`    Iterations: ${iterations.values.count || 'N/A'}`);
    }

    console.log('-'.repeat(65));
    console.log('  RELIABILITY');
    if (evalFailRate && evalFailRate.values) {
        const failPct = (evalFailRate.values.rate * 100).toFixed(2);
        const passPct = (100 - evalFailRate.values.rate * 100).toFixed(2);
        console.log(`    Success:    ${passPct}%`);
        console.log(`    Failures:   ${failPct}%`);
    }

    console.log('='.repeat(65) + '\n');

    return {};
}
