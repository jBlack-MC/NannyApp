CREATE TABLE IF NOT EXISTS notification_outbox (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 user_id INT NOT NULL,
 title VARCHAR(150) NOT NULL,
 message VARCHAR(500) NOT NULL,
 url VARCHAR(255) NULL,
 delivered_at DATETIME NULL,
 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 INDEX idx_notification_pending (delivered_at,id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS storage_deletions (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 file_path VARCHAR(255) NOT NULL,
 attempts INT NOT NULL DEFAULT 0,
 available_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 completed_at DATETIME NULL,
 INDEX idx_storage_pending (completed_at,available_at)
) ENGINE=InnoDB;
