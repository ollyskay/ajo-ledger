CREATE TABLE accounts (
    id          UUID PRIMARY KEY,
    type        VARCHAR(30) NOT NULL,
    owner_id    UUID,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE ledger_entries (
    id              UUID PRIMARY KEY,
    transaction_id  UUID NOT NULL,
    account_id      UUID NOT NULL REFERENCES accounts (id),
    amount_kobo     BIGINT NOT NULL CHECK (amount_kobo <> 0),
    currency        VARCHAR(3) NOT NULL DEFAULT 'NGN',
    description     VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_ledger_entries_transaction_id ON ledger_entries (transaction_id);
CREATE INDEX idx_ledger_entries_account_id ON ledger_entries (account_id);
