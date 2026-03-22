CREATE TABLE payments (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL,
    idempotency_key     VARCHAR(255) NOT NULL,
    amount              BIGINT NOT NULL,          -- stored in smallest unit (paise/cents)
    currency            VARCHAR(3) NOT NULL,
    status              VARCHAR(20) NOT NULL,
    request_hash        VARCHAR(64),              -- SHA-256 of request body
    gateway_payment_id  VARCHAR(100),             -- for polling UNKNOWN payments
    gateway_response    JSONB,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at        TIMESTAMPTZ,

    CONSTRAINT uq_payments_user_idempotency UNIQUE (user_id, idempotency_key)
);

CREATE INDEX idx_payments_status ON payments (status);
CREATE INDEX idx_payments_user_id ON payments (user_id);
CREATE INDEX idx_payments_created_at ON payments (created_at);
CREATE INDEX idx_payments_gateway_id ON payments (gateway_payment_id);