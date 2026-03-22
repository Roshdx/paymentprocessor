# Architecture Decision Records

## ADR-001: Dual-layer idempotency (Redis + Postgres)

**Decision**: Idempotency is enforced at two independent layers.

**Redis layer**: On every request, `SET idempotency:{userId}:{key} NX EX 86400`.
If the key exists, return the cached response immediately. Fast, no DB hit.

**Postgres layer**: A unique constraint on `(user_id, idempotency_key)` ensures
that even if Redis is down or evicts the key under memory pressure, the DB
rejects a duplicate INSERT with a constraint violation — which we catch and
convert to a 200 with the original response.

**Alternatives considered**:
- Redis only: Eviction under memory pressure could lose the key → double charge.
- Postgres only: Every request hits the DB even on cache hit — unnecessary latency.

**Tradeoff**: Redis is the fast path. Postgres is the guarantee.
The system degrades gracefully if Redis is unavailable — slower, not incorrect.

---

## ADR-002: UNKNOWN state for gateway timeouts

**Decision**: When the gateway call times out, write `UNKNOWN` — not `FAILED`.

**Why not FAILED**: If Stripe processed the charge before the connection dropped,
writing FAILED would let the client retry and create a duplicate charge.

**Resolution path**: A scheduled polling job queries the gateway every 5 minutes
for all UNKNOWN payments. It resolves them to SUCCESS or FAILED once the gateway
returns a definitive status. After 48 hours with no resolution, the payment is
escalated to manual review — never auto-failed.

**Tradeoff**: UNKNOWN adds operational complexity but eliminates double-charge risk.
A human reviews anything the system cannot resolve automatically.

---

## ADR-003: State machine with explicit transition validation

**Decision**: `PaymentStatus` enforces valid transitions at the enum level.
Any invalid transition (e.g. FAILED → PROCESSING) throws `InvalidTransitionException`
before touching the database.

**Why**: Silent invalid transitions are harder to debug than loud exceptions.
In money systems, unexpected state is always worse than a thrown exception.

**Valid transitions**:
```
PENDING    → PROCESSING
PROCESSING → SUCCESS | FAILED | UNKNOWN
UNKNOWN    → SUCCESS | FAILED
SUCCESS    → REFUNDED
FAILED     → (terminal)
REFUNDED   → (terminal)
```

---

## ADR-004: Amount stored as BIGINT in smallest currency unit

**Decision**: `amount` is stored as `BIGINT` representing paise (or cents).
₹149.99 is stored as `14999`. Conversion happens only at the API boundary.

**Why**: Floating point arithmetic on money is non-deterministic.
`0.1 + 0.2 != 0.3` in IEEE 754. A rounding error on ₹1 across 10 million
transactions is ₹10,000,000 of unexplained discrepancy.

---

## ADR-005: Reconciliation as a nightly batch job

**Decision**: Reconciliation runs as a scheduled job at 02:00 daily,
not as inline post-processing after each payment.

**Why not inline**: Adds ~200ms latency per payment (gateway API call),
couples the critical path to gateway availability, and adds no real-time
value since discrepancies require human review regardless.

**Tradeoff**: Discrepancies are caught up to 24 hours late.
Acceptable — real-time reconciliation is an enterprise-tier concern.