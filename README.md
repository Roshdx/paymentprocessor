# Idempotent Payment Processor

A production-grade payment processing system built around one guarantee:
a payment is processed **exactly once**, no matter how many times the client retries.

Designed to reflect how fintech companies like Razorpay and Stripe solve
the double-charge problem at scale.

---

## The core problem

A payment API that naively retries on failure will charge a customer twice.
This system solves that with:

- **Idempotency keys** enforced at two layers: Redis (fast path) + Postgres unique constraint (safety net)
- **A strict payment state machine** — invalid transitions throw, not silently fail
- **UNKNOWN state** for gateway timeouts — never auto-fail a payment you can't confirm
- **Nightly reconciliation** that diffs your DB against the gateway ledger and flags every rupee discrepancy

---

## Architecture
```
Client → API Gateway → Idempotency Guard (Redis)
                              ↓ cache miss
                       Payment Service → Postgres (PENDING)
                              ↓
                          Kafka Queue
                              ↓
                       Payment Worker → External Gateway (Stripe)
                              ↓
                    SUCCESS / FAILED / UNKNOWN
                              ↓ (nightly)
                       Reconciliation Job
```

---

## Tech stack

- **Java 21**, Spring Boot 3.2
- **PostgreSQL 16** — payment records, event log, unique constraint idempotency
- **Redis 7** — idempotency key cache with TTL
- **Apache Kafka** — async payment queue with dead letter topic
- **Flyway** — versioned DB migrations
- **Prometheus + Actuator** — metrics
- **Testcontainers** — integration tests with real infrastructure

---

## Running locally
```bash
make up          # start Postgres, Redis, Kafka
make api         # run the application on :8080
```

## Running tests
```bash
make test-unit          # pure logic — state machine, idempotency guard
make test-integration   # full flow with real DB, Redis, Kafka via Testcontainers
make test-all           # everything
```

## API
```
POST /api/v1/payments
Headers: Idempotency-Key: <uuid>
Body:    { "userId": "...", "amount": 14999, "currency": "INR" }

GET  /api/v1/payments/{id}
```

---

## Key design decisions

See [ADR.md](ADR.md) for the full reasoning behind each major decision.

---

## CHANGELOG

See [CHANGELOG.md](CHANGELOG.md).