// k6 run stress.js
// k6 run stress.js -e VUS=50 -e ITERATIONS=100

import http from 'k6/http';
import { check } from 'k6';

const BASE_URL    = __ENV.BASE_URL    || 'http://localhost:8080/api/v1';
const EMAIL       = __ENV.EMAIL       || 'cleanuser@verveguard.com';
const PASSWORD    = __ENV.PASSWORD    || 'Admin123!';
const VUS         = parseInt(__ENV.VUS        || '10');
const ITERATIONS  = parseInt(__ENV.ITERATIONS || '2');

export const options = {
    scenarios: {
        default: {
            executor: 'per-vu-iterations',
            vus: VUS,
            iterations: ITERATIONS,
        },
    },
};

export function setup() {
    const res = http.post(
        `${BASE_URL}/auth/login`,
        JSON.stringify({ email: EMAIL, password: PASSWORD }),
        { headers: { 'Content-Type': 'application/json' } }
    );
    if (res.status !== 200) throw new Error(`Login failed: ${res.status}`);
    return { token: res.json('data.accessToken') };
}

export default function (data) {
    const merchantId = Math.floor(Math.random() * 20) + 1;
    const amount = Math.floor(Math.random() * 9900) + 100;

    const res = http.post(
        `${BASE_URL}/fraud/evaluate/${merchantId}`,
        JSON.stringify({ amount, currency: 'NGN', cardNumber: `4${String(merchantId).padStart(15, '0')}` }),
        {
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${data.token}`,
            },
        }
    );
    check(res, { 'status 200': (r) => r.status === 200 });
}

export function handleSummary(data) {
    const d = data.metrics.http_req_duration?.values || {};
    const reqs = data.metrics.http_reqs?.values || {};
    const fails = data.metrics.http_req_failed?.values?.rate || 0;

    console.log(`
══════════════════════════════════════════════════
STRESS SMOKE TEST — ${VUS} VUs × ${ITERATIONS} iterations
══════════════════════════════════════════════════
LATENCY (ms)
  avg  : ${d.avg?.toFixed(2) || 'N/A'}
  min  : ${d.min?.toFixed(2) || 'N/A'}
  max  : ${d.max?.toFixed(2) || 'N/A'}
  p90  : ${d['p(90)']?.toFixed(2) || 'N/A'}
  p95  : ${d['p(95)']?.toFixed(2) || 'N/A'}

THROUGHPUT
  total: ${reqs.count || 0} requests
  rps  : ${reqs.rate?.toFixed(2) || 'N/A'} req/s

ERRORS
  fail : ${(fails * 100).toFixed(2)}%
══════════════════════════════════════════════════`);
    return {};
}
