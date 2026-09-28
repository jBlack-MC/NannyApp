-- Apply after v6. No existing application data is removed.
CREATE TABLE IF NOT EXISTS email_outbox (
    id CHAR(32) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    user_id INT NOT NULL,
    purpose VARCHAR(16) NOT NULL,
    token_hash CHAR(64) CHARACTER SET ascii NOT NULL,
    payload TEXT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'pending',
    attempts INT NOT NULL DEFAULT 0,
    available_at DATETIME NOT NULL,
    expires_at DATETIME NOT NULL,
    lease_token CHAR(32) NULL,
    lease_until DATETIME NULL,
    provider_id VARCHAR(64) NULL,
    last_code VARCHAR(40) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_email_due (status, available_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
