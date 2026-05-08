#!/usr/bin/env node

/**
 * VerveguardAPI Fraud Detection Demo
 *
 * Demonstrates each fraud detection scenario using the /fraud/evaluate endpoint.
 *
 * Scoring System:
 *   - Block threshold: 70 points
 *   - Review threshold: 30 points
 *   - Velocity gate: 30 points (triggers after 3 txns/60s)
 *   - Transaction limit gate: 25 points
 *   - Location anomaly gate: 35 points
 *   - Time window gate: variable (outside 6am-10pm)
 *
 * Scenarios:
 *   1. Merchant Blacklisted     - Uses testmerchant2@verveguard.com (blacklisted in seed)
 *   2. Velocity Trigger         - 4 rapid evaluations with same card (threshold: 3/60s)
 *   3. Combined Block           - Velocity + Over-limit + Location anomaly = BLOCKED
 *   4. Single Limit Exceeded    - Amount above tier limit (TIER_1: 10,000 NGN) = 25 points
 *   5. After Hours              - Transaction outside 06:00-22:00
 *
 * Usage:
 *   node fraud-demo.js                        # Run all scenarios
 *   node fraud-demo.js --scenario 1           # Run specific scenario
 *   node fraud-demo.js --list                 # List all scenarios
 *   node fraud-demo.js --delay 2000           # Delay between scenarios (ms)
 */

const args = process.argv.slice(2);
const config = {
    baseUrl: 'http://localhost:8080/api/v1',
    password: 'Admin123!',
    delayBetweenScenarios: 3000,
    targetScenario: null,
};

for (let i = 0; i < args.length; i++) {
    switch (args[i]) {
        case '--base-url': case '-u': config.baseUrl = args[++i]; break;
        case '--scenario': case '-s': config.targetScenario = parseInt(args[++i]); break;
        case '--delay': case '-d': config.delayBetweenScenarios = parseInt(args[++i]); break;
        case '--list': case '-l':
            console.log(`
Fraud Detection Scenarios:
  1. Merchant Blacklisted    - Hard block via blacklist gate
  2. Velocity Trigger        - Soft flag after 3 txns/60s (30 points)
  3. Combined Block          - Velocity + Over-limit + Location = BLOCKED (90 points)
  4. Single Limit Exceeded   - Soft flag, TIER_1 limit is 10,000 NGN (25 points)
  5. After Hours             - Soft flag outside 06:00-22:00
`);
            process.exit(0);
        case '--help': case '-h':
            console.log(`
Usage: node fraud-demo.js [options]

Options:
  --scenario, -s    Run a specific scenario (1-5)
  --base-url, -u    API base URL (default: http://localhost:8080/api/v1)
  --delay, -d       Delay between scenarios in ms (default: 3000)
  --list, -l        List all scenarios
  --help, -h        Show help
`);
            process.exit(0);
    }
}

// -----------------------------------------------------------------------------
// Helpers
// -----------------------------------------------------------------------------

function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

function header(title) {
    console.log('\n' + '='.repeat(61));
    console.log(`  ${title}`);
    console.log('='.repeat(61));
}

function subheader(title) {
    console.log(`\n  +-- ${title}`);
}

function result(label, value, highlight = false) {
    const icon = highlight ? '  ! ' : '  | ';
    console.log(`${icon}${label.padEnd(20)} ${value}`);
}

function divider() {
    console.log('  +' + '-'.repeat(59));
}

async function login(email, password = config.password) {
    const res = await fetch(`${config.baseUrl}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password })
    });
    if (!res.ok) {
        const text = await res.text();
        throw new Error(`Login failed for ${email}: ${res.status} - ${text}`);
    }
    const data = await res.json();
    return data.data.accessToken;
}

async function evaluateFraud(token, body, clientIp = null) {
    const start = performance.now();
    const headers = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
    };
    if (clientIp) {
        headers['X-Forwarded-For'] = clientIp;
    }

    const res = await fetch(`${config.baseUrl}/fraud/evaluate`, {
        method: 'POST',
        headers,
        body: JSON.stringify(body)
    });
    const ms = (performance.now() - start).toFixed(0);

    let payload = null;
    try { payload = await res.json(); } catch { /* ignore */ }

    return { status: res.status, ms, payload };
}

function statusLabel(status) {
    if (status === 200) return '200 OK';
    if (status === 403) return '403 FORBIDDEN';
    if (status === 429) return '429 TOO MANY REQUESTS';
    return `${status}`;
}

function fraudLabel(payload) {
    if (payload?.data) {
        return payload.data;
    }
    const msg = payload?.errorMessage || payload?.message || '';
    if (!msg) return '-';
    return msg;
}

function fraudIcon(status) {
    if (status === 'CLEAN') return '[OK]';
    if (status === 'SUSPICIOUS') return '[!!]';
    if (status === 'BLOCKED') return '[XX]';
    return '[??]';
}

// -----------------------------------------------------------------------------
// Scenario Runners
// -----------------------------------------------------------------------------

/**
 * Scenario 1: Blacklisted Merchant
 * testmerchant2@verveguard.com is blacklisted in the seed data
 */
async function scenarioBlacklisted() {
    header('SCENARIO 1 - Merchant Blacklisted (Hard Block)');
    console.log(`
  Rule:     Merchant is on the blacklist (hard block gate)
  Expected: BLOCKED status
  Merchant: testmerchant2@verveguard.com (blacklisted in seed)
`);

    subheader('Evaluating transaction as blacklisted merchant...');
    try {
        const token = await login('testmerchant2@verveguard.com');
        const clientIp = `10.${Math.floor(Math.random() * 255)}.${Math.floor(Math.random() * 255)}.${Math.floor(Math.random() * 255)}`;

        const res = await evaluateFraud(token, {
            amount: 1000,
            currency: 'NGN',
            cardNumber: '4222222222222222'  // merchant2's card (blacklisted)
        }, clientIp);

        const fraudStatus = fraudLabel(res.payload);
        const isBlocked = fraudStatus === 'BLOCKED';
        result('HTTP Status',   statusLabel(res.status), res.status !== 200);
        result('Response ms',   `${res.ms}ms`);
        result('Fraud Status',  `${fraudIcon(fraudStatus)} ${fraudStatus}`, !isBlocked);
        if (isBlocked) {
            result('Result', 'Blacklist gate triggered correctly', false);
        }
    } catch (e) {
        result('Error', e.message, true);
    }
    divider();
}

/**
 * Scenario 2: Velocity Trigger
 * 4 rapid evaluations using the same card - triggers velocity gate (30 points -> SUSPICIOUS)
 * Threshold: 3 transactions in 60 seconds
 */
async function scenarioVelocity() {
    header('SCENARIO 2 - Velocity Trigger (Soft Flag)');
    console.log(`
  Rule:     > 3 transactions with same card in 60 seconds
  Score:    30 points (review threshold = 30)
  Expected: 1st-3rd CLEAN, 4th SUSPICIOUS
  Merchant: merchant1@stresstest.com (TIER_3)
`);

    const token = await login('merchant1@stresstest.com');
    const requests = 4;
    // Use unique IP to avoid location anomaly interference
    const clientIp = `10.${Math.floor(Math.random() * 255)}.${Math.floor(Math.random() * 255)}.${Math.floor(Math.random() * 255)}`;

    subheader(`Firing ${requests} rapid evaluations with same card...`);
    for (let i = 1; i <= requests; i++) {
        const res = await evaluateFraud(token, {
            amount: 100,  // Small amount to stay under limits
            currency: 'NGN',
            cardNumber: '4000000000000001'  // merchant1's card
        }, clientIp);

        const fraudStatus = fraudLabel(res.payload);
        const flagged = fraudStatus !== 'CLEAN';
        result(`Evaluation #${i}`, `${statusLabel(res.status)} ${fraudIcon(fraudStatus)} ${fraudStatus}  (${res.ms}ms)`, flagged);
    }
    console.log(`\n  Note: 4th transaction triggers velocity gate (30 points = SUSPICIOUS)`);
    divider();
}

/**
 * Scenario 3: Combined Block
 * Build up velocity, then trigger over-limit + location anomaly = BLOCKED
 * Velocity (30) + Transaction limit (25) + Location anomaly (35) = 90 points > 70 block threshold
 */
async function scenarioCombinedBlock() {
    header('SCENARIO 3 - Combined Block (Multiple Gates)');
    console.log(`
  Rule:     Velocity + Over-limit + Location anomaly
  Scores:   Velocity=30 + Limit=25 + Location=35 = 90 points
  Threshold: Block at 70 points
  Expected: First 3 CLEAN (building velocity), 4th BLOCKED
  Merchant: merchant2@stresstest.com (TIER_3)
`);

    const token = await login('merchant2@stresstest.com');
    // US IP for building velocity (consistent location)
    const usIp = '8.8.8.8';
    // Russian IP for final request (triggers location anomaly)
    const ruIp = '77.88.8.8';

    subheader('Building velocity with 3 small transactions...');
    for (let i = 1; i <= 3; i++) {
        const res = await evaluateFraud(token, {
            amount: 100,
            currency: 'NGN',
            cardNumber: '4000000000000002'
        }, usIp);

        const fraudStatus = fraudLabel(res.payload);
        result(`Buildup #${i}`, `${statusLabel(res.status)} ${fraudIcon(fraudStatus)} ${fraudStatus}  (${res.ms}ms)`);
    }

    subheader('Final request: huge amount + different country...');
    const res = await evaluateFraud(token, {
        amount: 9999999,  // Way over any tier limit
        currency: 'NGN',
        cardNumber: '4000000000000002'
    }, ruIp);

    const fraudStatus = fraudLabel(res.payload);
    const isBlocked = fraudStatus === 'BLOCKED';
    result('HTTP Status',   statusLabel(res.status), res.status !== 200);
    result('Response ms',   `${res.ms}ms`);
    result('Fraud Status',  `${fraudIcon(fraudStatus)} ${fraudStatus}`, !isBlocked);
    if (isBlocked) {
        result('Result', 'Combined gates triggered: velocity + limit + location', false);
    }
    divider();
}

/**
 * Scenario 4: Single Transaction Limit Exceeded
 * TIER_1 single limit is 10,000 NGN
 * Send 15,000 NGN - should add 25 points (under review threshold of 30)
 * Note: This alone doesn't trigger SUSPICIOUS, need to combine with other factors
 */
async function scenarioSingleLimit() {
    header('SCENARIO 4 - Single Limit Exceeded');
    console.log(`
  Rule:     Amount exceeds merchant's tier single transaction limit
  Tier:     TIER_1 - single limit: 10,000 NGN
  Amount:   15,000 NGN
  Score:    25 points (under review threshold of 30)
  Expected: CLEAN (25 < 30 review threshold)
  Merchant: testmerchant@verveguard.com (TIER_1)

  Note: Transaction limit alone (25 points) doesn't reach review
        threshold (30 points). Combine with velocity to see SUSPICIOUS.
`);

    subheader('Evaluating 15,000 NGN transaction on 10,000 NGN limit...');
    try {
        const token = await login('testmerchant@verveguard.com');
        const clientIp = `10.${Math.floor(Math.random() * 255)}.${Math.floor(Math.random() * 255)}.${Math.floor(Math.random() * 255)}`;

        const res = await evaluateFraud(token, {
            amount: 15000,
            currency: 'NGN',
            cardNumber: '4111111111111111'
        }, clientIp);

        const fraudStatus = fraudLabel(res.payload);
        result('HTTP Status',   statusLabel(res.status), res.status !== 200);
        result('Response ms',   `${res.ms}ms`);
        result('Fraud Status',  `${fraudIcon(fraudStatus)} ${fraudStatus}`);
        result('Explanation',   '25 points < 30 review threshold = CLEAN');
    } catch (e) {
        result('Error', e.message, true);
    }
    divider();
}

/**
 * Scenario 5: After Hours
 * Transaction outside 06:00-22:00
 * Uses merchant3 (clean) with normal amount
 */
async function scenarioAfterHours() {
    header('SCENARIO 5 - After Hours Transaction');

    const hour = new Date().getHours();
    const isAfterHours = hour < 6 || hour >= 22;

    console.log(`
  Rule:     Transaction outside 06:00-22:00 adds time window score
  Current:  ${new Date().toLocaleTimeString()} (${isAfterHours ? 'AFTER HOURS - rule will fire' : 'WITHIN HOURS - rule will NOT fire'})
  Expected: ${isAfterHours ? 'SUSPICIOUS or higher (depends on score)' : 'CLEAN (no time penalty)'}
  Merchant: merchant3@stresstest.com
`);

    if (!isAfterHours) {
        console.log('  Note: Current time is within business hours (06:00-22:00).');
        console.log('        Re-run after 22:00 or before 06:00 to trigger the time gate.');
        console.log('        Executing anyway to show a clean evaluation for contrast.\n');
    }

    subheader('Evaluating transaction...');
    try {
        const token = await login('merchant3@stresstest.com');
        const clientIp = `10.${Math.floor(Math.random() * 255)}.${Math.floor(Math.random() * 255)}.${Math.floor(Math.random() * 255)}`;

        const res = await evaluateFraud(token, {
            amount: 2000,
            currency: 'NGN',
            cardNumber: '4000000000000003'
        }, clientIp);

        const fraudStatus = fraudLabel(res.payload);
        result('HTTP Status',  statusLabel(res.status), res.status !== 200);
        result('Response ms',  `${res.ms}ms`);
        result('Fraud Status', `${fraudIcon(fraudStatus)} ${fraudStatus}`, fraudStatus !== 'CLEAN' || isAfterHours);
        if (!isAfterHours && fraudStatus === 'CLEAN') {
            result('Note', 'Run after 22:00 to trigger time window gate', true);
        }
    } catch (e) {
        result('Error', e.message, true);
    }
    divider();
}

// -----------------------------------------------------------------------------
// Main
// -----------------------------------------------------------------------------

const scenarios = [
    { id: 1, name: 'Merchant Blacklisted',      fn: scenarioBlacklisted  },
    { id: 2, name: 'Velocity Trigger',          fn: scenarioVelocity     },
    { id: 3, name: 'Combined Block',            fn: scenarioCombinedBlock },
    { id: 4, name: 'Single Limit Exceeded',     fn: scenarioSingleLimit  },
    { id: 5, name: 'After Hours Transaction',   fn: scenarioAfterHours   },
];

async function main() {
    console.log('\n' + '='.repeat(61));
    console.log('        VerveguardAPI - Fraud Detection Demo');
    console.log('='.repeat(61));
    console.log(`\n  Base URL:  ${config.baseUrl}`);
    console.log(`  Endpoint:  POST /fraud/evaluate`);
    console.log(`  Time:      ${new Date().toLocaleString()}`);
    console.log(`\n  Scoring: block>=70, review>=30`);
    console.log(`  Gates:   velocity=30, limit=25, location=35, blacklist=hard`);

    const toRun = config.targetScenario
        ? scenarios.filter(s => s.id === config.targetScenario)
        : scenarios;

    if (toRun.length === 0) {
        console.error(`\n  [X] Unknown scenario: ${config.targetScenario}. Use --list to see options.`);
        process.exit(1);
    }

    console.log(`\n  Running: ${toRun.map(s => `#${s.id} ${s.name}`).join(', ')}\n`);

    for (let i = 0; i < toRun.length; i++) {
        await toRun[i].fn();
        if (i < toRun.length - 1) {
            console.log(`\n  ... Waiting ${config.delayBetweenScenarios / 1000}s before next scenario...\n`);
            await sleep(config.delayBetweenScenarios);
        }
    }

    console.log('\n' + '='.repeat(61));
    console.log('  Demo complete.');
    console.log('='.repeat(61) + '\n');
}

main().catch(err => {
    console.error('\n[X] Demo failed:', err.message);
    process.exit(1);
});
