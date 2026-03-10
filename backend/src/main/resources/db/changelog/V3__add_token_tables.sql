-- ── Refresh tokens ───────────────────────────────────────────────────────────
CREATE TABLE refresh_tokens (
    id          UUID            NOT NULL DEFAULT gen_random_uuid(),
    token       VARCHAR(255)    NOT NULL,
    user_id     UUID            NOT NULL,
    expires_at  TIMESTAMPTZ     NOT NULL,
    revoked     BOOLEAN         NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_refresh_tokens    PRIMARY KEY (id),
    CONSTRAINT uq_refresh_token     UNIQUE (token),
    CONSTRAINT fk_refresh_user      FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_token   ON refresh_tokens (token);
CREATE INDEX idx_refresh_tokens_user    ON refresh_tokens (user_id);

-- ── Token blacklist ───────────────────────────────────────────────────────────
CREATE TABLE token_blacklist (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    token           VARCHAR(1024)   NOT NULL,
    expires_at      TIMESTAMPTZ     NOT NULL,
    blacklisted_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_token_blacklist   PRIMARY KEY (id),
    CONSTRAINT uq_blacklist_token   UNIQUE (token)
);

CREATE INDEX idx_blacklist_token      ON token_blacklist (token);
CREATE INDEX idx_blacklist_expires_at ON token_blacklist (expires_at);

