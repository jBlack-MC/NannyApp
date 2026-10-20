-- Shared revocation version for web sessions and Android bearer tokens.
ALTER TABLE users ADD COLUMN IF NOT EXISTS session_version INT NOT NULL DEFAULT 0;
ALTER TABLE api_tokens ADD COLUMN IF NOT EXISTS session_version INT NOT NULL DEFAULT 0;

-- A request is an attempt, not proof that installation completed.
CREATE TABLE IF NOT EXISTS apk_download_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source VARCHAR(32) NOT NULL DEFAULT 'web',
    delivery VARCHAR(32) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_apk_download_created (created_at)
) ENGINE=InnoDB;

-- Small prototype uploads can persist in the same DB on stateless hosts.
CREATE TABLE IF NOT EXISTS stored_media (
    path VARCHAR(255) PRIMARY KEY,
    mime VARCHAR(80) NOT NULL,
    data_base64 MEDIUMTEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;
