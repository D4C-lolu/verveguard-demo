#!/usr/bin/env node

/**
 * VerveguardAPI Fraud Detection Demo
 *
 * Demonstrates each fraud detection scenario using the /fraud/evaluate endpoint.
 *
 * Scenarios:
 *   1. Merchant Blacklisted     - Uses testmerchant@verveguard.com (blacklisted in seed)
 *   2. IP Rate Limit            - 6 rapid requests from same IP (threshold: 5/min)
 *   3. Card Velocity            - 4 rapid evaluations with same card (threshold: 3/60s)
 *   4. Single Limit Exceeded    - Amount above TIER_1 single limit (10,000 NGN)
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
  1. Merchant Blacklisted    - Hard block, BLOCKED status
  2. IP Rate Limit           - Hard block after 5 req/min
  3. Card Velocity           - Soft flag / block after 3 txns/60s
  4. Single Limit Exceeded   - Soft flag, TIER_1 limit is 10,000 NGN
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

function generateUUID() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
        const r = Math.random() * 16 | 0;
        return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
    });
}

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
        // Successful response - data contains fraud status
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
 * testmerchant@verveguard.com is blacklisted in the seed data
 */
async function scenarioBlacklisted() {
    header('SCENARIO 1 - Merchant Blacklisted (Hard Block)');
    console.log(`
  Rule:     Merchant is on the blacklist
  Expected: BLOCKED status
  Merchant: testmerchant@verveguard.com (blacklisted in seed)
`);

    subheader('Evaluating transaction as blacklisted merchant...');
    try {
        const token = await login('testmerchant@verveguard.com');
        const res = await evaluateFraud(token, {
            amount: 1000,
            currency: 'NGN',
            cardNumber: '4111111111111111'
        }, '10.0.0.100');

        const fraudStatus = fraudLabel(res.payload);
        result('HTTP Status',   statusLabel(res.status), res.status !== 200);
        result('Response ms',   `${res.ms}ms`);
        result('Fraud Status',  `${fraudIcon(fraudStatus)} ${fraudStatus}`, fraudStatus !== 'CLEAN');
    } catch (e) {
        result('Error', e.message, true);
    }
    divider();
}

/**
 * Scenario 2: IP Rate Limit
 * Fire 7 rapid requests - should block after the 5th
 * Uses merchant1 (clean, TIER_3)
 */
async function scenarioRateLimit() {
    header('SCENARIO 2 - IP Rate Limit (Hard Block)');
    console.log(`
  Rule:     > 5 requests per minute from same IP
  Expected: First 5 pass, 6th+ blocked
  Merchant: merchant1@stresstest.com
`);

    const token = await login('merchant1@stresstest.com');
    const requests = 7;
    const clientIp = `10.${Math.floor(Math.random() * 255)}.${Math.floor(Math.random() * 255)}.${Math.floor(Math.random() * 255)}`;

    subheader(`Firing ${requests} rapid evaluations from IP ${clientIp}...`);
    for (let i = 1; i <= requests; i++) {
        const res = await evaluateFraud(token, {
            amount: 100,
            currency: 'NGN',
            cardNumber: '4000000000000001'
        }, clientIp);

        const fraudStatus = fraudLabel(res.payload);
        const blocked = fraudStatus === 'BLOCKED' || res.status !== 200;
        result(`Request #${i}`, `${statusLabel(res.status)} ${fraudIcon(fraudStatus)} ${fraudStatus}  (${res.ms}ms)`, blocked);
    }
    divider();
}

/**
 * Scenario 3: Card Velocity
 * 4 rapid evaluations using the same card - should flag/block after 3rd
 * Uses merchant2 with their own card
 */
async function scenarioCardVelocity() {
    header('SCENARIO 3 - Card Velocity (Soft Flag -> Block)');
    console.log(`
  Rule:     > 3 transactions with same card in 60 seconds
  Expected: 1st-3rd pass (possibly SUSPICIOUS), 4th BLOCKED
  Merchant: merchant2@stresstest.com
`);

    const token = await login('merchant2@stresstest.com');
    const requests = 4;
    // Use unique IP to avoid rate limit interference
    const clientIp = `10.${Math.floor(Math.random() * 255)}.${Math.floor(Math.random() * 255)}.${Math.floor(Math.random() * 255)}`;

    subheader(`Firing ${requests} rapid evaluations with same card...`);
    for (let i = 1; i <= requests; i++) {
        const res = await evaluateFraud(token, {
            amount: 500,
            currency: 'NGN',
            cardNumber: '4000000000000002'
        }, clientIp);

        const fraudStatus = fraudLabel(res.payload);
        const flagged = fraudStatus !== 'CLEAN';
        result(`Evaluation #${i}`, `${statusLabel(res.status)} ${fraudIcon(fraudStatus)} ${fraudStatus}  (${res.ms}ms)`, flagged);
    }
    divider();
}

/**
 * Scenario 4: Single Transaction Limit Exceeded
 * TIER_1 single limit is 10,000 NGN
 * Send 15,000 NGN - should flag as SUSPICIOUS
 */
async function scenarioSingleLimit() {
    header('SCENARIO 4 - Single Limit Exceeded (Soft Flag)');
    console.log(`
  Rule:     Amount exceeds merchant's tier single transaction limit
  Tier:     TIER_1 - single limit: 10,000 NGN
  Amount:   15,000 NGN
  Expected: SUSPICIOUS status
  Merchant: demo.merchant@verveguard.com (TIER_1)
`);

    subheader('Evaluating 15,000 NGN transaction on 10,000 NGN limit...');
    try {
        const token = await login('demo.merchant@verveguard.com');
        // Use unique IP
        const clientIp = `10.${Math.floor(Math.random() * 255)}.${Math.floor(Math.random() * 255)}.${Math.floor(Math.random() * 255)}`;

        const res = await evaluateFraud(token, {
            amount: 15000,
            currency: 'NGN',
            cardNumber: '4011111111111111'
        }, clientIp);

        const fraudStatus = fraudLabel(res.payload);
        result('HTTP Status',   statusLabel(res.status), res.status !== 200);
        result('Response ms',   `${res.ms}ms`);
        result('Fraud Status',  `${fraudIcon(fraudStatus)} ${fraudStatus}`, fraudStatus !== 'CLEAN');
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
    header('SCENARIO 5 - After Hours Transaction (Soft Flag)');

    const hour = new Date().getHours();
    const isAfterHours = hour < 6 || hour >= 22;

    console.log(`
  Rule:     Transaction outside 06:00-22:00
  Current:  ${new Date().toLocaleTimeString()} (${isAfterHours ? 'AFTER HOURS - rule will fire' : 'WITHIN HOURS - rule will NOT fire'})
  Expected: ${isAfterHours ? 'SUSPICIOUS status' : 'CLEAN status'}
  Merchant: merchant3@stresstest.com
`);

    if (!isAfterHours) {
        console.log('  Note: Current time is within business hours.');
        console.log('        Re-run after 22:00 or before 06:00 to trigger the rule.');
        console.log('        Executing anyway to show a clean evaluation for contrast.\n');
    }

    subheader('Evaluating transaction...');
    try {
        const token = await login('merchant3@stresstest.com');
        // Use unique IP
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
            result('Note', 'Run after 22:00 to trigger SUSPICIOUS flag', true);
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
    { id: 2, name: 'IP Rate Limit',             fn: scenarioRateLimit    },
    { id: 3, name: 'Card Velocity',             fn: scenarioCardVelocity },
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
