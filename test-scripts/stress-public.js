import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

// Custom metrics
const evaluateDuration = new Trend('evaluate_duration', true);
const loginDuration = new Trend('login_duration', true);
const evaluateFailRate = new Rate('evaluate_fail_rate');

// Global cache to store tokens so we don't spam Login
const tokenCache = {};

export const options = {
    stages: [
        { duration: '20s', target: 50 },  // Ramp up
        { duration: '40s', target: 100 }, // Load
        { duration: '10s', target: 0 },   // Scale down
    ],
    thresholds: {
        'evaluate_duration': ['p(95)<500'],   // Evaluate p95 under 500ms
        'evaluate_fail_rate': ['rate<0.01'],  // Less than 1% failures
        'login_duration': ['p(95)<500'],      // Login p95 under 500ms
    },
};

export default function () {
    const BASE_URL = 'http://localhost:8080/api/v1';

    // Use the VU (Virtual User) ID to rotate through different merchants
    const merchantIndex = (__VU % 200) + 1;
    const email = `merchant${merchantIndex}@stresstest.com`;

    // 1. GET OR CREATE TOKEN
    let token = tokenCache[email];

    if (!token) {
        const loginRes = http.post(
            `${BASE_URL}/auth/login`,
            JSON.stringify({ email, password: 'Admin123!' }),
            { headers: { 'Content-Type': 'application/json' } }
        );

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

    // 2. PREPARE FRAUD EVALUATION PAYLOAD
    // FraudEvaluationRequest: amount, currency, cardNumber
    const payload = JSON.stringify({
        amount: 100 + Math.floor(Math.random() * 900),
        currency: 'NGN',
        cardNumber: `4${String(merchantIndex).padStart(15, '0')}`
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`,
            'X-Forwarded-For': `192.168.${(__VU % 256)}.${(__ITER % 256)}`
        },
        tags: { name: 'fraud-evaluate' },
    };

    // 3. FRAUD EVALUATION - uses /fraud/evaluate (merchant context from token)
    const res = http.post(`${BASE_URL}/fraud/evaluate`, payload, params);

    evaluateDuration.add(res.timings.duration);

    const success = check(res, {
        'evaluate status 200': (r) => r.status === 200,
    });

    evaluateFailRate.add(!success);

    if (!success) {
        console.error(`FAILED EVALUATE: VU=${__VU} merchant=${email} status=${res.status} body=${res.body.substring(0, 120)}`);
    }

    // Pacing to prevent the VU from slamming the server too hard
    sleep(0.5);
}
