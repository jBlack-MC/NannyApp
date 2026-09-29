-- Additive MariaDB migration. No demo data or destructive duplicate cleanup.
CREATE TABLE IF NOT EXISTS admin_profiles (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    user_id      INT          NOT NULL UNIQUE,
    access_level ENUM('super_admin','support','moderator') NOT NULL DEFAULT 'support',
    department   VARCHAR(100) DEFAULT NULL,
    phone_ext    VARCHAR(20)  DEFAULT NULL,
    notes        TEXT         DEFAULT NULL,
    created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_admin_profile_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS check_in_code      VARCHAR(6)   DEFAULT NULL AFTER status,
    ADD COLUMN IF NOT EXISTS check_in_attempts  TINYINT UNSIGNED NOT NULL DEFAULT 0 AFTER check_in_code,
    ADD COLUMN IF NOT EXISTS checked_in_at      DATETIME     DEFAULT NULL AFTER check_in_attempts,
    ADD COLUMN IF NOT EXISTS checked_out_at     DATETIME     DEFAULT NULL AFTER checked_in_at,
    ADD COLUMN IF NOT EXISTS parent_confirmed_at DATETIME    DEFAULT NULL AFTER checked_out_at,
    ADD COLUMN IF NOT EXISTS dispute_reason     VARCHAR(500) DEFAULT NULL AFTER parent_confirmed_at,
    ADD COLUMN IF NOT EXISTS disputed_at        DATETIME     DEFAULT NULL AFTER dispute_reason;

ALTER TABLE bookings
    MODIFY COLUMN status ENUM('pending','confirmed','in_progress','completed','rejected','cancelled','disputed')
    NOT NULL DEFAULT 'pending';

ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS payout_status ENUM('held','released','refunded') DEFAULT NULL AFTER status,
    ADD COLUMN IF NOT EXISTS released_at   DATETIME DEFAULT NULL AFTER payout_status;
