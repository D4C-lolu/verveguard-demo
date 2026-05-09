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

---

## Available Scripts

| Script | Description | Tool |
|--------|-------------|------|
| `stress.js` | Fraud evaluation load test | k6 |
| `fraud-demo.js` | Interactive fraud scenario demo | Node.js |

---

## k6 Stress Test (`stress.js`)

Tests the fraud evaluation endpoint under load. Supports three executor modes, two scenarios, and is fully configured via `-e` flags.

### Executor Modes

| `EXECUTOR` | Behaviour |
|---|---|
| `constant` *(default)* | Fixed VU count for a set duration |
| `ramp` | Ramps up → sustains at peak → ramps down |
| `iterations` | Spreads a fixed request count across VUs |

### Scenarios

| `SCENARIO` | Behaviour |
|---|---|
| `merchant` *(default)* | Each VU authenticates as one of N merchants with its own token |
| `admin` | All VUs share a single admin token |

### Run Commands

```bash
# Default (constant load, merchant scenario)
k6 run stress.js

# Custom base URL
k6 run stress.js -e BASE_URL=http://staging:8080/api/v1

# Admin scenario
k6 run stress.js -e SCENARIO=admin

# Ramp: 0 → 200 VUs over 30s, sustain for 1m
k6 run stress.js -e EXECUTOR=ramp -e MAX_VUS=200 -e RAMP_DURATION=30s -e SUSTAIN=1m

# Fixed iterations: 50 VUs share 1000 total requests
k6 run stress.js -e EXECUTOR=iterations -e VUS=50 -e ITERATIONS=1000

# High throughput: 200 VUs constant for 2 minutes
k6 run stress.js -e VUS=200 -e DURATION=2m
```

### All Flags

| Flag | Default | Modes |
|---|---|---|
| `BASE_URL` | `http://localhost:8080/api/v1` | all |
| `SCENARIO` | `merchant` | all |
| `EXECUTOR` | `constant` | all |
| `PASSWORD` | `Admin123!` | all |
| `NUM_MERCHANTS` | `20` | all |
| `THINK_MS` | `300` | all |
| `VUS` | `200` | `constant`, `iterations` |
| `DURATION` | `1m` | `constant` |
| `MAX_VUS` | falls back to `VUS` | `ramp` |
| `RAMP_DURATION` | `30s` | `ramp` |
| `SUSTAIN` | falls back to `DURATION` | `ramp` |
| `ITERATIONS` | `1000` | `iterations` |

### Thresholds

The run is marked failed if either threshold is breached:

| Metric | Threshold |
|---|---|
| `evaluate_duration` p95 | < 2000ms |
| `evaluate_fail_rate` | < 5% |

### Output Options

```bash
# Detailed percentile breakdown
k6 run stress.js --summary-trend-stats="avg,min,med,max,p(90),p(95),p(99)"

# Export raw results to JSON
k6 run stress.js --out json=results.json

# Export summary to JSON
k6 run stress.js --summary-export=summary.json

# Live web dashboard (k6 v0.49+)
k6 run stress.js --out web-dashboard
```

---

## Fraud Demo (`fraud-demo.js`)

Interactive walkthrough of fraud detection scenarios. Each scenario sends a crafted request and prints the engine's response alongside the expected result.

### Run Commands

```bash
# Run all scenarios
node fraud-demo.js

# Run a specific scenario by number
node fraud-demo.js --scenario 1

# List available scenarios
node fraud-demo.js --list

# Custom base URL and delay between requests
node fraud-demo.js --base-url http://staging:8080/api/v1 --delay 5000
```

### Scenarios

| # | Name | Expected Result |
|---|---|---|
| 1 | Merchant Blacklisted | BLOCKED (hard block) |
| 2 | Velocity Trigger | SUSPICIOUS (30 points) |
| 3 | Combined Block | BLOCKED (90 points) |
| 4 | Single Limit Exceeded | CLEAN (25 points, below 30 threshold) |
| 5 | After Hours | SUSPICIOUS (run after 22:00) |

---

## Tips

- **Check logs** while a test is running: `docker logs -f verveguard-demo`
- **Redis health**: Velocity counting depends on Redis — verify it's up before running velocity scenarios
- **Token expiry**: If a long test returns sudden 401s, reduce `DURATION` or pre-issue longer-lived tokens