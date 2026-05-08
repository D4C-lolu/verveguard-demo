# Test Scripts

## Prerequisites

- **k6**: Install from https://k6.io/docs/get-started/installation/
  ```bash
  # Windows (winget)
  winget install k6

  # Windows (chocolatey)
  choco install k6

  # macOS
  brew install k6

  # Linux (Debian/Ubuntu)
  sudo gpg -k
  sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
  echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
  sudo apt-get update
  sudo apt-get install k6
  ```

- **Node.js**: Required for `fraud-demo.js` (v18+)
- **JMeter**: Required for `fraud_test.jmx` (optional)

## Available Scripts

| Script | Description | Tool |
|--------|-------------|------|
| `stress.js` | Transfer endpoint load test | k6 |
| `stress-public.js` | Fraud evaluation load test | k6 |
| `fraud-demo.js` | Interactive fraud scenario demo | Node.js |
| `fraud_test.jmx` | JMeter fraud load test | JMeter |

---

## k6 Stress Tests

### 1. Transfer Stress Test (`stress.js`)

Tests the `/transfers/me` endpoint with 100 concurrent merchants.

```bash
# Basic run
k6 run stress.js

# With custom options
k6 run --vus 50 --duration 30s stress.js

# Output to JSON for analysis
k6 run --out json=results.json stress.js

# With web dashboard (k6 v0.49+)
k6 run --out web-dashboard stress.js
```

**Thresholds:**
- Transfer p95 latency < 1000ms
- Transfer failure rate < 1%
- Login p95 latency < 500ms

### 2. Fraud Evaluation Stress Test (`stress-public.js`)

Tests the `/fraud/evaluate` endpoint with authenticated merchants.

```bash
# Basic run
k6 run stress-public.js

# Higher load
k6 run --vus 200 --duration 60s stress-public.js

# With stages override
k6 run -e RAMP_UP=30s -e LOAD=60s stress-public.js
```

**Thresholds:**
- Evaluate p95 latency < 500ms
- Evaluate failure rate < 1%
- Login p95 latency < 500ms

### Common k6 Options

```bash
# Virtual users and duration
k6 run --vus 100 --duration 1m script.js

# Iterations instead of duration
k6 run --vus 50 --iterations 1000 script.js

# Environment variables
k6 run -e BASE_URL=http://staging:8080/api/v1 script.js

# Output formats
k6 run --out json=results.json script.js
k6 run --out csv=results.csv script.js
k6 run --out influxdb=http://localhost:8086/k6 script.js

# Summary export
k6 run --summary-export=summary.json script.js

# Quiet mode (less output)
k6 run --quiet script.js

# Verbose mode
k6 run --verbose script.js
```

---

## Fraud Demo (`fraud-demo.js`)

Interactive demonstration of fraud detection scenarios.

```bash
# Run all scenarios
node fraud-demo.js

# Run specific scenario
node fraud-demo.js --scenario 1

# List available scenarios
node fraud-demo.js --list

# Custom delay between scenarios
node fraud-demo.js --delay 5000

# Different base URL
node fraud-demo.js --base-url http://staging:8080/api/v1
```

**Scenarios:**
1. **Merchant Blacklisted** - Hard block via blacklist gate
2. **Velocity Trigger** - Soft flag after 3 txns/60s (30 points)
3. **Combined Block** - Velocity + Over-limit + Location = BLOCKED
4. **Single Limit Exceeded** - Over tier limit (25 points, not enough alone)
5. **After Hours** - Time window gate (run after 22:00)

---

## JMeter Test (`fraud_test.jmx`)

```bash
# GUI mode (for development)
jmeter -t fraud_test.jmx

# CLI mode (for actual testing)
jmeter -n -t fraud_test.jmx -l results.jtl -e -o report/

# With custom properties
jmeter -n -t fraud_test.jmx -Jthreads=200 -Jduration=120
```

---

## Interpreting Results

### k6 Output

```
     data_received..................: 1.2 MB 20 kB/s
     data_sent......................: 234 kB 3.9 kB/s
     http_req_duration..............: avg=45ms min=12ms max=234ms p(90)=78ms p(95)=95ms
     http_reqs......................: 5000   83/s
     iteration_duration.............: avg=520ms min=450ms max=1.2s
     vus............................: 100    min=0 max=100
     vus_max........................: 100    min=100 max=100
```

**Key metrics:**
- `http_req_duration`: Response time (watch p95)
- `http_reqs`: Throughput (requests/second)
- `http_req_failed`: Error rate (should be < 1%)

### Custom Metrics

The scripts define custom metrics:
- `transfer_duration` / `evaluate_duration`: Isolated endpoint timing
- `login_duration`: Auth overhead
- `transfer_fail_rate` / `evaluate_fail_rate`: Business failure rate

---

## Tips

1. **Warm up the app** before stress testing:
   ```bash
   k6 run --vus 10 --duration 10s stress.js
   ```

2. **Monitor during tests** - Watch Grafana dashboard at http://localhost:3000

3. **Check application logs** if failures spike:
   ```bash
   docker logs -f verveguard-demo
   ```

4. **Database connections** - High VU counts may exhaust connection pools

5. **Redis** - Velocity counting and rate limiting depend on Redis being healthy
