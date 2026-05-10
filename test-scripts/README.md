```markdown
# Test Scripts

## Prerequisites

- **k6**: Install from https://k6.io/docs/get-started/installation/
  ```bash
  # Windows (winget)
  winget install k6

  # macOS
  brew install k6

  # Linux (Debian/Ubuntu)
  sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg \
    --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
  echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" \
    | sudo tee /etc/apt/sources.list.d/k6.list
  sudo apt-get update && sudo apt-get install k6
  ```

- **Node.js**: Required for `fraud-demo.js` (v18+)
- **Docker**: Required if running k6 inside the container network (recommended)

---

## How It Works

### The Two-Phase Test Design

Both `stress.js` and `overhead.js` use a **warmup → measure** pattern:

```
Phase 1 — Warmup (not measured)
  Ramps from 0 → full VUs over 15s
  Sustains for 15s at full load
  Ramps back down over 5s
  Goal: fill connection pools, trigger JIT compilation, warm caches

Phase 2 — Measure (reported)
  Starts after warmup completes (~35s in)
  Runs at full VUs for the configured DURATION
  Only this phase's data appears in the summary
```

This ensures the JVM is hot before any numbers are recorded. Cold JVM results are misleading — the first few seconds of a Spring app under load look much worse than steady state.

### Why Run k6 Inside Docker?

When k6 runs on WSL or Windows and the app runs in Docker, traffic crosses the WSL/Windows/Docker NAT bridge. Under high concurrency this bridge adds latency and caps throughput — not because the app is slow, but because the networking layer between them is.

Running k6 as a Docker container on the **same network** as the app eliminates this entirely. Requests go container-to-container over Docker's internal bridge — no NAT, no host networking stack.

**Rule of thumb**: always use the Docker command for benchmark results. WSL results will show ~50% lower RPS on the same server.

### How Metrics Are Recorded

k6 exposes three custom metrics:

| Metric | Type | What it measures |
|---|---|---|
| `evaluate_duration` | Trend | End-to-end latency per request (ms) |
| `evaluate_fail_rate` | Rate | Fraction of non-200 responses |
| `evaluate_count` | Counter | Total requests sent in measure phase |

These are only written during the `measureFn` — warmup traffic is tagged separately and excluded from the summary.

---

## Available Scripts

| Script | Description | Tool |
|--------|-------------|------|
| `stress.js` | Fraud evaluation load test with warmup | k6 |
| `overhead.js` | Pure framework overhead via `/ping` | k6 |
| `fraud-demo.js` | Interactive fraud scenario demo | Node.js |

---

## Running via Docker (Recommended)

Find your Docker network name:
```bash
docker network ls
```

Look for the network your app is on — typically `verveguard-demo_default`.

Run any script through Docker k6:
```bash
# overhead test
docker run --rm -i \
  --network verveguard-demo_default \
  grafana/k6 run \
  -e BASE_URL=http://app:8080/api/v1 \
  -e VUS=200 -e DURATION=30s \
  - < overhead.js

# stress test
docker run --rm -i \
  --network verveguard-demo_default \
  grafana/k6 run \
  -e BASE_URL=http://app:8080/api/v1 \
  -e VUS=200 -e DURATION=2m \
  - < stress.js
```

Note `- < script.js` — the `-` tells k6 to read from stdin, and `< script.js` pipes the file in. This is required because the file lives on the host, not inside the k6 container.

The hostname `app` resolves to your Spring container via Docker DNS. Use the `container_name` from your `docker-compose.yml` if it differs.

---

## Overhead Test (`overhead.js`)

Measures pure framework cost — Spring Security filter chain, interceptors, AOP, response serialization — with **zero business logic** by hitting `/fraud/ping`.

No auth header is sent, so the JWT filter skips immediately. What you're measuring is the raw cost of a request passing through the full Spring stack.

### Run Commands

```bash
# Local
k6 run overhead.js -e VUS=200 -e DURATION=30s -e THRESHOLD=100

# Docker (recommended)
docker run --rm -i --network verveguard-demo_default grafana/k6 run \
  -e BASE_URL=http://app:8080/api/v1 \
  -e VUS=200 -e DURATION=30s -e THRESHOLD=100 \
  - < overhead.js
```

### Flags

| Flag | Default | Description |
|---|---|---|
| `BASE_URL` | `http://localhost:8080/api/v1` | Target host |
| `VUS` | `200` | Concurrent virtual users |
| `DURATION` | `30s` | Measure phase duration |
| `THRESHOLD` | `100` | p95 target in ms |

### Interpreting Overhead Results

The overhead test has one job: tell you what the framework costs before any business logic runs.

```
p50 : 5ms    ← half your requests cost this much — your typical case
p90 : 12ms   ← 90% of requests are faster than this
p95 : 18ms   ← your SLA target lives here
p99 : 45ms   ← tail latency — GC pauses, scheduler jitter
max : 210ms  ← worst single request in the run
```

**What good looks like**: p95 under 100ms at 200 VUs means the framework adds less than 100ms to every request before business logic runs.

**What to investigate**:
- p50 fast but p95 slow → tail latency issue, likely GC or thread scheduling
- All percentiles slow → CPU saturation or something in the filter chain blocking
- High max with normal p99 → occasional spike, usually GC or cold connection

---

## Stress Test (`stress.js`)

Tests the full fraud evaluation pipeline under sustained load — JWT auth, DB queries, Redis velocity checks, GeoIP lookup, scoring engine, consequence handling.

### How Scenarios Work

**merchant** (default): Each VU logs in as a different merchant and uses its own token. Tokens are distributed round-robin across VUs. Realistic — mirrors how the system actually runs in production.

**admin**: All VUs share a single admin token and target merchant-specific evaluation endpoints. Useful for isolating the evaluation engine without the overhead of per-merchant auth.

### Executor Modes

| `EXECUTOR` | Behaviour | Use when |
|---|---|---|
| `constant` | Fixed VUs for a set duration | Steady-state throughput measurement |
| `ramp` | 0 → peak → 0 | Finding the breaking point |
| `iterations` | Fixed total requests across all VUs | Repeatable comparison runs |

### Run Commands

```bash
# Default — 200 VUs, 1 minute, merchant scenario
k6 run stress.js

# Admin scenario — all VUs share one token
k6 run stress.js -e SCENARIO=admin -e VUS=200 -e DURATION=2m

# Ramp — find where the service degrades
k6 run stress.js -e EXECUTOR=ramp -e MAX_VUS=200 -e RAMP_DURATION=30s -e SUSTAIN=1m

# Fixed iterations — repeatable benchmark
k6 run stress.js -e EXECUTOR=iterations -e VUS=50 -e ITERATIONS=1000

# No think time — maximum pressure
k6 run stress.js -e VUS=200 -e DURATION=2m -e THINK_MS=0

# Docker
docker run --rm -i --network verveguard-demo_default grafana/k6 run \
  -e BASE_URL=http://app:8080/api/v1 \
  -e SCENARIO=admin -e VUS=200 -e DURATION=2m \
  - < stress.js
```

### All Flags

| Flag | Default | Description |
|---|---|---|
| `BASE_URL` | `http://localhost:8080/api/v1` | Target host |
| `SCENARIO` | `merchant` | `merchant` or `admin` |
| `EXECUTOR` | `constant` | `constant`, `ramp`, or `iterations` |
| `PASSWORD` | `Admin123!` | Auth password |
| `NUM_MERCHANTS` | `20` | Merchant accounts to log in as |
| `THINK_MS` | `300` | Pause between requests per VU (ms) |
| `VUS` | `200` | Concurrent virtual users |
| `DURATION` | `1m` | Measure phase duration |
| `MAX_VUS` | falls back to `VUS` | Peak VUs for ramp mode |
| `RAMP_DURATION` | `30s` | Time to ramp up |
| `SUSTAIN` | falls back to `DURATION` | Time at peak in ramp mode |
| `ITERATIONS` | `1000` | Total requests in iterations mode |

### Interpreting Stress Results

The summary prints both raw numbers and an English interpretation. Here's how to read them:

**Latency**
```
p50 : 320ms   ← typical request cost under this load
p90 : 980ms   ← most requests land here
p95 : 1400ms  ← your effective SLA at this load level
p99 : 3200ms  ← worst 1% — often DB lock contention or GC
max : 8100ms  ← single worst request, usually an outlier
```

**What the numbers mean for a fraud evaluation service**:

| p95 range | Interpretation |
|---|---|
| < 200ms | Excellent — well within real-time tolerance |
| 200–500ms | Good — acceptable for a synchronous fraud check |
| 500ms–1s | Acceptable — monitor for growth under higher load |
| 1–2s | Degraded — noticeable to downstream systems |
| > 2s | Struggling — investigate DB, Redis, or CPU saturation |

**Throughput**
```
RPS   : 83.37 req/s    ← total requests per second including warmup traffic
total : 10352          ← requests completed in the measure phase
```

**Errors**
```
fail : 0.00%   ← fraction of non-200 responses
```

Zero errors at any RPS is the most important result. A service that's slow but never fails is in a much better position than one that's fast but drops requests under load.

**The OBSERVATIONS block** translates all of this into plain English automatically — use it as your starting point when reading results.

### What to Watch During a Test

In a separate terminal while k6 runs:

```bash
watch -n 2 '
echo "=== HIKARI ===" && \
curl -s http://localhost:8080/actuator/metrics/hikaricp.connections.active | grep -o "\"value\":[0-9.]*" && \
curl -s http://localhost:8080/actuator/metrics/hikaricp.connections.pending | grep -o "\"value\":[0-9.]*" && \
echo "=== CPU ===" && \
curl -s http://localhost:8080/actuator/metrics/process.cpu.usage | grep -o "\"value\":[0-9.]*"
'
```

| Signal | Meaning |
|---|---|
| `hikaricp.connections.pending` > 0 | Threads waiting for a DB connection — pool too small |
| `process.cpu.usage` > 0.9 | CPU saturated — scale horizontally or reduce load |
| `executor.queued` > 0 | Async consequence queue backing up |
| All metrics normal but latency high | Queueing at the network or Tomcat accept queue |

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

## Output Options

```bash
# Detailed percentile breakdown
k6 run stress.js --summary-trend-stats="avg,min,med,max,p(50),p(90),p(95),p(99)"

# Export raw results to JSON
k6 run stress.js --out json=results.json

# Export summary to JSON
k6 run stress.js --summary-export=summary.json

# Live web dashboard (k6 v0.49+)
k6 run stress.js --out web-dashboard
```

---

## Tips

- **Always use Docker k6 for benchmark results** — WSL networking halves throughput and misrepresents server performance
- **Discard the first run** — even with warmup, the very first test after a fresh container start may be skewed by JVM class loading. Run twice, use the second result
- **Check logs while running**: `docker logs -f verveguard-demo`
- **Redis health**: Velocity counting depends on Redis — verify it's up before running velocity scenarios
- **Token expiry**: If a long test returns sudden 401s, reduce `DURATION` or pre-issue longer-lived tokens
- **THINK_MS=0 is maximum pressure** — default 300ms think time means VUs are idle most of the time. Use `THINK_MS=0` only to find the ceiling, not as a realistic load profile
```