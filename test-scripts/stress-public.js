import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

// Custom metrics - Removed loginDuration as it's no longer used
const evaluateDuration = new Trend('evaluate_duration', true);
const evaluateFailRate = new Rate('evaluate_fail_rate');

export const options = {
    stages: [
        { duration: '20s', target: 50 },  // Ramp up
        { duration: '40s', target: 100 }, // Load
        { duration: '10s', target: 0 },   // Scale down
    ],
    thresholds: {
        'evaluate_duration': ['p(95)<500'],   // Evaluate p95 under 500ms
        'evaluate_fail_rate': ['rate<0.01'],  // Less than 1% failures
    },
};

export default function () {
    const BASE_URL = 'http://localhost:8080/api/v1';

    // Use the VU (Virtual User) ID to rotate through different merchant IDs
    const merchantId = (__VU % 200) + 1;

    // 1. PREPARE PAYLOAD
    const now = new Date().toISOString();
    const payload = JSON.stringify({
        transactionId: `k6-${__VU}-${__ITER}-${Date.now()}`,
        accountNumber: `33${String(merchantId).padStart(8, '0')}`,
        amount: 100 + Math.floor(Math.random() * 900),
        currency: 'NGN',
        cardNumber: `4${String(merchantId).padStart(15, '0')}`, // Note: Logic still hashes this server-side
        ipAddress: `192.168.${(__VU % 256)}.${(__ITER % 256)}`,
        transactionTime: now
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            // Authorization header removed as the endpoint is now public
        },
        tags: { name: 'fraud-evaluate-public' },
    };

    // 2. FRAUD EVALUATION
    // Updated to use the new path variable: /fraud/evaluate/{merchantId}
    const res = http.post(`${BASE_URL}/fraud/evaluate/${merchantId}`, payload, params);

    evaluateDuration.add(res.timings.duration);

    const success = check(res, {
        'evaluate status 200': (r) => r.status === 200,
    });

    evaluateFailRate.add(!success);

    if (!success) {
        console.error(`FAILED EVALUATE: VU=${__VU} merchantId=${merchantId} status=${res.status} body=${res.body.substring(0, 120)}`);
    }

    // Pacing to prevent the VU from slamming the server too hard
    sleep(0.5);
}