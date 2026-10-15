-- Additive. Nanny-App Plus (Google Play subscriptions), profile-view history,
-- priority bookings and the machine-translation cache.
CREATE TABLE IF NOT EXISTS subscriptions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    platform VARCHAR(20) NOT NULL DEFAULT 'google_play',
    product_id VARCHAR(100) NOT NULL,
    purchase_token VARCHAR(512) NOT NULL,
    token_hash CHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    expires_at DATETIME NULL,
    auto_renewing TINYINT(1) NOT NULL DEFAULT 0,
    last_verified_at DATETIME NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_subscription_token (token_hash),
    INDEX idx_subscription_user (user_id, expires_at),
    CONSTRAINT fk_subscription_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;
-- One row per parent per nanny per day, so "who viewed your profile" is not flooded by repeat visits.
CREATE TABLE IF NOT EXISTS profile_views (
    nanny_id INT NOT NULL,
    viewer_id INT NOT NULL,
    view_date DATE NOT NULL,
    viewed_at DATETIME NOT NULL,
    PRIMARY KEY (nanny_id, viewer_id, view_date),
    INDEX idx_profile_views_recent (nanny_id, viewed_at),
    CONSTRAINT fk_profile_view_nanny FOREIGN KEY (nanny_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_profile_view_viewer FOREIGN KEY (viewer_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS is_priority TINYINT(1) NOT NULL DEFAULT 0;
CREATE TABLE IF NOT EXISTS translation_cache (
    text_hash CHAR(64) NOT NULL,
    target VARCHAR(10) NOT NULL,
    translated TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (text_hash, target)
) ENGINE=InnoDB;
