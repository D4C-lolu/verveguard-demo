# Test Scripts

## Prerequisites

- **k6**: Install from https://k6.io/docs/get-started/installation/
  ```bash
  # Windows (winget)
  winget install k6

  # macOS
  brew install k6

  # Linux (Debian/Ubuntu)
  sudo apt-get install k6
  ```

- **Node.js**: Required for `fraud-demo.js` (v18+)

## Available Scripts

| Script | Description | Tool |
|--------|-------------|------|
| `stress.js` | Fraud evaluation load test with warm-up | k6 |
| `fraud-demo.js` | Interactive fraud scenario demo | Node.js |

---

## k6 Stress Test (`stress.js`)

Two-phase stress test:
1. **Warm-up**: Authenticates all merchants sequentially (avoids login bottleneck)
2. **Load**: Hammers the fraud evaluation endpoint with cached tokens

### Run Commands

```bash
# Merchant scenario (default)
k6 run stress.js

# Admin scenario
k6 run -e SCENARIO=admin stress.js

# Custom base URL
k6 run -e BASE_URL=http://staging:8080/api/v1 stress.js

# Fixed number of requests (e.g., 200 total)
k6 run --vus 10 --iterations 200 stress.js

# High throughput (~200 req/s for 30 seconds)
k6 run --vus 50 --duration 30s stress.js
```

### Test Phases

| Phase | Duration | VUs | Description |
|-------|----------|-----|-------------|
| Warm-up | ~30s | 5 | Authenticate 50 merchants sequentially |
| Ramp-up | 10s | 0→20 | Start load |
| Sustain | 30s | 40 | Full load |
| Ramp-down | 10s | 40→0 | Cool down |

### Thresholds

| Metric | Threshold |
|--------|-----------|
| `evaluate_duration` p95 | < 1000ms |
| `evaluate_fail_rate` | < 5% |

---

## Fraud Demo (`fraud-demo.js`)

Interactive demonstration of fraud detection scenarios.

```bash
# Run all scenarios
node fraud-demo.js

# Run specific scenario
node fraud-demo.js --scenario 1

# List scenarios
node fraud-demo.js --list

# Custom settings
node fraud-demo.js --delay 5000 --base-url http://staging:8080/api/v1
```

### Scenarios

| # | Name | Expected Result |
|---|------|-----------------|
| 1 | Merchant Blacklisted | BLOCKED (hard block) |
| 2 | Velocity Trigger | SUSPICIOUS (30 points) |
| 3 | Combined Block | BLOCKED (90 points) |
| 4 | Single Limit Exceeded | CLEAN (25 points < 30 threshold) |
| 5 | After Hours | SUSPICIOUS (run after 22:00) |

---

## Common k6 Options

```bash
# Override load profile
k6 run --vus 20 --duration 30s stress.js

# Fixed iterations (e.g., exactly 200 requests)
k6 run --vus 10 --iterations 200 stress.js

# 200 concurrent requests (all at once)
k6 run --vus 200 --iterations 200 stress.js

# Show detailed percentile stats
k6 run --vus 10 --iterations 200 --summary-trend-stats="avg,min,med,max,p(90),p(95),p(99)" stress.js

# Output to JSON for analysis
k6 run --out json=results.json stress.js

# Web dashboard (k6 v0.49+)
k6 run --out web-dashboard stress.js

# Summary export
k6 run --summary-export=summary.json stress.js
```

---

## Tips

1. **Monitor during tests**: Watch Grafana at http://localhost:3000
2. **Check logs**: `docker logs -f verveguard-demo`
3. **Redis health**: Velocity counting depends on Redis
