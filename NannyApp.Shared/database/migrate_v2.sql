-- Additive MariaDB migration. No demo data or destructive duplicate cleanup.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS date_of_birth DATE         DEFAULT NULL AFTER phone,
    ADD COLUMN IF NOT EXISTS address       VARCHAR(255) DEFAULT NULL AFTER date_of_birth,
    ADD COLUMN IF NOT EXISTS gender        ENUM('male','female','non-binary','prefer_not_to_say') DEFAULT NULL AFTER address;

ALTER TABLE parent_profiles
    ADD COLUMN IF NOT EXISTS emergency_contact_name         VARCHAR(100) DEFAULT NULL AFTER emergency_contact,
    ADD COLUMN IF NOT EXISTS emergency_contact_relationship VARCHAR(50)  DEFAULT NULL AFTER emergency_contact_name;

ALTER TABLE nanny_profiles
    ADD COLUMN IF NOT EXISTS gender           ENUM('male','female','non-binary','prefer_not_to_say') DEFAULT NULL AFTER bio,
    ADD COLUMN IF NOT EXISTS date_of_birth    DATE          DEFAULT NULL AFTER gender,
    ADD COLUMN IF NOT EXISTS banner_image     VARCHAR(255)  DEFAULT NULL AFTER photo_url,
    ADD COLUMN IF NOT EXISTS languages        VARCHAR(255)  DEFAULT 'English' AFTER skills,
    ADD COLUMN IF NOT EXISTS qualifications   TEXT          DEFAULT NULL AFTER languages,
    ADD COLUMN IF NOT EXISTS specialisations  VARCHAR(255)  DEFAULT NULL AFTER qualifications,
    ADD COLUMN IF NOT EXISTS profile_views    INT UNSIGNED  DEFAULT 0 AFTER specialisations;

ALTER TABLE nanny_profiles ADD UNIQUE KEY IF NOT EXISTS uq_nanny_user (user_id);

ALTER TABLE payments ADD UNIQUE KEY IF NOT EXISTS uq_pay_booking   (booking_id);
ALTER TABLE reviews  ADD UNIQUE KEY IF NOT EXISTS uq_rev_booking   (booking_id, reviewer_id);

ALTER TABLE bookings
    ADD INDEX IF NOT EXISTS idx_b_parent   (parent_id),
    ADD INDEX IF NOT EXISTS idx_b_nanny    (nanny_id),
    ADD INDEX IF NOT EXISTS idx_b_status   (status),
    ADD INDEX IF NOT EXISTS idx_b_datetime (date_time);

ALTER TABLE chat_messages ADD INDEX IF NOT EXISTS idx_cm_recv_read (receiver_id, is_read);
ALTER TABLE notifications ADD INDEX IF NOT EXISTS idx_n_user_read  (user_id, is_read);
ALTER TABLE reviews        ADD INDEX IF NOT EXISTS idx_r_nanny      (nanny_id);

ALTER TABLE nanny_profiles
    ADD INDEX IF NOT EXISTS idx_np_verif  (verification_status),
    ADD INDEX IF NOT EXISTS idx_np_rating (average_rating);

CREATE TABLE IF NOT EXISTS children (
    id                   INT AUTO_INCREMENT PRIMARY KEY,
    parent_id            INT              NOT NULL,
    name                 VARCHAR(100)     NOT NULL,
    age                  TINYINT UNSIGNED DEFAULT NULL,
    gender               ENUM('male','female','other') DEFAULT NULL,
    allergies            VARCHAR(500)     DEFAULT NULL,
    medical_conditions   VARCHAR(500)     DEFAULT NULL,
    special_needs        VARCHAR(500)     DEFAULT NULL,
    favourite_activities VARCHAR(500)     DEFAULT NULL,
    notes_for_nannies    TEXT             DEFAULT NULL,
    created_at           TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_child_parent FOREIGN KEY (parent_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS saved_nannies (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    parent_id   INT NOT NULL,
    nanny_id    INT NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY  uq_save (parent_id, nanny_id),
    CONSTRAINT fk_save_parent FOREIGN KEY (parent_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_save_nanny  FOREIGN KEY (nanny_id)  REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS nanny_availability (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    nanny_id     INT     NOT NULL,
    day_of_week  TINYINT NOT NULL COMMENT '0=Sun 1=Mon 2=Tue 3=Wed 4=Thu 5=Fri 6=Sat',
    is_available TINYINT(1) NOT NULL DEFAULT 1,
    time_start   TIME    DEFAULT '08:00:00',
    time_end     TIME    DEFAULT '18:00:00',
    UNIQUE KEY   uq_avail_day (nanny_id, day_of_week),
    CONSTRAINT fk_avail_nanny FOREIGN KEY (nanny_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS nanny_portfolio (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    nanny_id       INT             NOT NULL,
    type           ENUM('certificate','id','photo','reference','other') NOT NULL DEFAULT 'certificate',
    title          VARCHAR(150)    NOT NULL,
    file_path      VARCHAR(255)    NOT NULL,
    admin_verified TINYINT(1)      NOT NULL DEFAULT 0,
    created_at     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_port_nanny FOREIGN KEY (nanny_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS page_content (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    page_key   VARCHAR(50)  NOT NULL UNIQUE,
    title      VARCHAR(200) DEFAULT NULL,
    body       MEDIUMTEXT   DEFAULT NULL,
    updated_by INT          DEFAULT NULL,
    updated_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_pc_admin FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;
