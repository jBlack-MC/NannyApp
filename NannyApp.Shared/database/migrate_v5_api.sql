-- Additive MariaDB migration. No demo data or destructive duplicate cleanup.
CREATE TABLE IF NOT EXISTS api_tokens (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT          NOT NULL,
    token_hash  CHAR(64)     NOT NULL UNIQUE, -- sha256(token), the raw token is never stored
    device_info VARCHAR(255) DEFAULT NULL,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    expires_at  DATETIME     NOT NULL,
    CONSTRAINT fk_apitoken_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_apitoken_user (user_id)
) ENGINE=InnoDB;
