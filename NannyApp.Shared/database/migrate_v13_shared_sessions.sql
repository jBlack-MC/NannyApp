-- Sessions must exist before access hardening, even before the first browser request.
CREATE TABLE IF NOT EXISTS web_sessions (
    id VARCHAR(128) NOT NULL PRIMARY KEY,
    data TEXT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_web_sessions_updated ON web_sessions (updated_at);
