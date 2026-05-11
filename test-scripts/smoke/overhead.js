// k6 run overhead.js
// k6 run overhead.js -e VUS=50 -e ITERATIONS=100

import http from 'k6/http';
import { check } from 'k6';

const BASE_URL    = __ENV.BASE_URL    || 'http://localhost:8080/api/v1';
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

export default function () {
    const res = http.get(`${BASE_URL}/fraud/ping`);
    check(res, {
        'status 200': (r) => r.status === 200,
        'body ok':    (r) => r.body.includes('ok'),
    });
}

export function handleSummary(data) {
    const d = data.metrics.http_req_duration?.values || {};
    const reqs = data.metrics.http_reqs?.values || {};
    const fails = data.metrics.http_req_failed?.values?.rate || 0;

    console.log(`
══════════════════════════════════════════════════
OVERHEAD SMOKE TEST — ${VUS} VUs × ${ITERATIONS} iterations
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
