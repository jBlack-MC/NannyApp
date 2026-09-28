-- Non-destructive; apply after v5 and phase2 authentication. Shared by web/API instances.
-- MariaDB syntax, consistent with v3. Ensures older optional-auth schemas also work.
ALTER TABLE users ADD COLUMN IF NOT EXISTS verification_sent_at DATETIME DEFAULT NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS remember_token VARCHAR(64) DEFAULT NULL;
ALTER TABLE password_resets ADD COLUMN IF NOT EXISTS used TINYINT(1) NOT NULL DEFAULT 0;
CREATE TABLE IF NOT EXISTS security_rate_limits (
    bucket CHAR(64) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    attempts INT UNSIGNED NOT NULL,
    resets_at BIGINT UNSIGNED NOT NULL,
    INDEX idx_security_expiry (resets_at)
) ENGINE=InnoDB;
-- Maintenance: DELETE FROM security_rate_limits WHERE resets_at < UNIX_TIMESTAMP() - 86400;
