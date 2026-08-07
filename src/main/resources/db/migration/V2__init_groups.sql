CREATE TABLE users (
    id          UUID PRIMARY KEY,
    phone       VARCHAR(20) NOT NULL UNIQUE,
    email       VARCHAR(255) UNIQUE,
    bvn_hash    VARCHAR(255),
    kyc_tier    SMALLINT NOT NULL DEFAULT 1,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE groups (
    id                  UUID PRIMARY KEY,
    name                VARCHAR(255) NOT NULL,
    contribution_kobo   BIGINT NOT NULL CHECK (contribution_kobo > 0),
    cycle               VARCHAR(10) NOT NULL CHECK (cycle IN ('WEEKLY', 'MONTHLY')),
    member_limit        INT NOT NULL CHECK (member_limit >= 2),
    status              VARCHAR(20) NOT NULL DEFAULT 'FORMING'
                            CHECK (status IN ('FORMING', 'ACTIVE', 'COMPLETED', 'DISSOLVED')),
    created_by          UUID NOT NULL REFERENCES users (id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE memberships (
    id                  UUID PRIMARY KEY,
    group_id            UUID NOT NULL REFERENCES groups (id),
    user_id             UUID NOT NULL REFERENCES users (id),
    payout_position     INT NOT NULL CHECK (payout_position > 0),
    joined_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                            CHECK (status IN ('ACTIVE', 'REMOVED')),
    UNIQUE (group_id, user_id),
    UNIQUE (group_id, payout_position)
);

CREATE TABLE cycles (
    id                          UUID PRIMARY KEY,
    group_id                    UUID NOT NULL REFERENCES groups (id),
    cycle_number                INT NOT NULL CHECK (cycle_number > 0),
    opens_at                    TIMESTAMPTZ NOT NULL,
    due_at                      TIMESTAMPTZ NOT NULL,
    beneficiary_membership_id   UUID NOT NULL REFERENCES memberships (id),
    status                      VARCHAR(20) NOT NULL DEFAULT 'OPEN'
                                    CHECK (status IN ('OPEN', 'SETTLED', 'DEFAULTED')),
    UNIQUE (group_id, cycle_number)
);

CREATE INDEX idx_memberships_group_id ON memberships (group_id);
CREATE INDEX idx_cycles_group_id ON cycles (group_id);
