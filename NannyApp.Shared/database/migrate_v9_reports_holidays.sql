-- Additive. Safety reports about a specific person are support tickets in the 'safety'
-- category, so they reach the existing admin queue first; these columns link the report
-- to the account and booking it concerns.
ALTER TABLE support_tickets
    ADD COLUMN IF NOT EXISTS reported_user_id INT NULL,
    ADD COLUMN IF NOT EXISTS booking_id INT NULL,
    ADD COLUMN IF NOT EXISTS report_reason VARCHAR(40) NULL;
CREATE INDEX IF NOT EXISTS idx_ticket_reported_user ON support_tickets (reported_user_id, created_at);
-- Public holidays fetched from the Nager.Date API, one row per country and year.
CREATE TABLE IF NOT EXISTS public_holidays_cache (
    country CHAR(2) NOT NULL,
    year SMALLINT NOT NULL,
    payload TEXT NOT NULL,
    fetched_at DATETIME NOT NULL,
    PRIMARY KEY (country, year)
) ENGINE=InnoDB;
