# Changelog

## [Unreleased]

## [1.0.0] - 2026-03-21
### Added
- `POST /api/v1/payments` with idempotency key enforcement
- Dual-layer idempotency: Redis NX + Postgres unique constraint
- Payment state machine with validated transitions
- Kafka-based async payment worker
- Exponential backoff with jitter for retryable gateway failures
- UNKNOWN state and gateway polling loop for timeout resolution
- Dead letter topic for exhausted retries
- Nightly reconciliation job with four discrepancy types
- Flyway migrations for payments and payment_events tables
- Prometheus metrics via Spring Actuator
- Unit tests for state machine and idempotency guard
- Integration tests using Testcontainers