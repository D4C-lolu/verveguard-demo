import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

// Custom metrics for granular timing (mirrors the Node.js stress test output)
const transferDuration = new Trend('transfer_duration', true);
const loginDuration = new Trend('login_duration', true);
const transferFailRate = new Rate('transfer_fail_rate');

// Global cache to store tokens so we don't spam Login
const tokenCache = {};

export const options = {
    stages: [
        { duration: '20s', target: 50 },  // Ramp up
        { duration: '40s', target: 100 }, // Load
        { duration: '10s', target: 0 },   // Scale down
    ],
    thresholds: {
        'transfer_duration': ['p(95)<1000'],  // Transfer-only p95 under 1s
        'transfer_fail_rate': ['rate<0.01'],  // Less than 1% transfer failures
        'login_duration': ['p(95)<500'],      // Login p95 under 500ms
    },
};

export default function () {
    const BASE_URL = 'http://localhost:8080/api/v1';
    const merchantIndex = (__VU % 200) + 1;
    const email = `merchant${merchantIndex}@stresstest.com`;

    // 1. GET OR CREATE TOKEN — timed separately
    let token = tokenCache[email];

    if (!token) {
        const loginRes = http.post(
            `${BASE_URL}/auth/login`,
            JSON.stringify({ email, password: 'Admin123!' }),
            { headers: { 'Content-Type': 'application/json' } }
        );

        // Record login time only on actual login attempts
        loginDuration.add(loginRes.timings.duration);

        if (loginRes.status === 200) {
            token = loginRes.json('data.accessToken');
            tokenCache[email] = token;
        } else {
            console.error(`FAILED LOGIN: ${email} - Status: ${loginRes.status}`);
            sleep(1);
            return;
        }
    }

    // 2. THE TRANSFER — timed in isolation
    const payload = JSON.stringify({
        fromAccountNumber: `33${String(merchantIndex).padStart(8, '0')}`,
        toAccountNumber: `33${String(merchantIndex + 200).padStart(8, '0')}`,
        amount: 100,
        currency: 'NGN',
        cardNumber: `4${String(merchantIndex).padStart(15, '0')}`,
        description: `k6 stress test VU=${__VU} ITER=${__ITER}`
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`,
            'X-Idempotency-Key': `k6-${__VU}-${__ITER}`
        },
        tags: { name: 'transfer' }, // Groups this request in k6 Cloud/output
    };

    const res = http.post(`${BASE_URL}/transfers/me`, payload, params);

    // Record transfer time regardless of outcome
    transferDuration.add(res.timings.duration);

    const success = check(res, {
        'transfer status 201': (r) => r.status === 201,
    });

    transferFailRate.add(!success);

    if (!success) {
        console.error(`FAILED TRANSFER: VU=${__VU} merchant=${email} status=${res.status} body=${res.body.substring(0, 120)}`);
    }

    sleep(0.5);
}