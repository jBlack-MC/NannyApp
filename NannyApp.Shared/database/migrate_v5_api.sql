-- =====================================================================
--  Nanny-App • v5 Migration — mobile API token auth
--  Run AFTER migrate_v4.sql: mysql -u root nanny_app < database/migrate_v5_api.sql
--
--  The existing web app authenticates with PHP sessions (cookies). The
--  native Android app instead authenticates with a bearer token sent in
--  the Authorization header on every /api/ request. This table maps an
--  opaque, randomly-generated token to a user until it's revoked
--  (logout) or expires (90 days of inactivity).
-- =====================================================================

USE nanny_app;

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
