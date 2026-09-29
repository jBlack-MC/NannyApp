-- Additive MariaDB migration. No demo data or destructive duplicate cleanup.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email_verified  TINYINT(1)   NOT NULL DEFAULT 0 AFTER status,
    ADD COLUMN IF NOT EXISTS verification_token VARCHAR(100) DEFAULT NULL AFTER email_verified,
    ADD COLUMN IF NOT EXISTS verification_sent_at DATETIME DEFAULT NULL AFTER verification_token,
    ADD COLUMN IF NOT EXISTS remember_token  VARCHAR(64)  DEFAULT NULL AFTER verification_sent_at;

ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS children_details TEXT         DEFAULT NULL AFTER notes,
    ADD COLUMN IF NOT EXISTS booking_address  VARCHAR(255) DEFAULT NULL AFTER children_details,
    ADD COLUMN IF NOT EXISTS booking_ref      VARCHAR(20)  DEFAULT NULL AFTER booking_address;

CREATE TABLE IF NOT EXISTS support_tickets (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT             DEFAULT NULL,
    name        VARCHAR(100)    NOT NULL,
    email       VARCHAR(150)    NOT NULL,
    category    ENUM('booking','payment','technical','safety','general') NOT NULL DEFAULT 'general',
    subject     VARCHAR(200)    NOT NULL,
    message     TEXT            NOT NULL,
    status      ENUM('open','in_progress','resolved','closed') NOT NULL DEFAULT 'open',
    admin_notes TEXT            DEFAULT NULL,
    created_at  TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ticket_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE INDEX IF NOT EXISTS idx_tickets_status   ON support_tickets (status);
CREATE INDEX IF NOT EXISTS idx_tickets_user     ON support_tickets (user_id);
CREATE INDEX IF NOT EXISTS idx_tickets_category ON support_tickets (category);

CREATE TABLE IF NOT EXISTS password_resets (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT          NOT NULL,
    token       VARCHAR(64)  NOT NULL UNIQUE,
    expires_at  DATETIME     NOT NULL,
    used        TINYINT(1)   NOT NULL DEFAULT 0,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reset_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX IF NOT EXISTS idx_reset_token ON password_resets (token);

CREATE TABLE IF NOT EXISTS email_verifications (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT          NOT NULL,
    token       VARCHAR(64)  NOT NULL UNIQUE,
    verified    TINYINT(1)   NOT NULL DEFAULT 0,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_verify_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS availability_slots (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    nanny_id    INT          NOT NULL,
    day_of_week TINYINT      NOT NULL COMMENT '0=Sun,1=Mon,...,6=Sat',
    slot        ENUM('morning','afternoon','evening') NOT NULL,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_slot (nanny_id, day_of_week, slot),
    CONSTRAINT fk_slot_nanny FOREIGN KEY (nanny_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;
