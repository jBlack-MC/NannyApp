-- Additive MariaDB migration. No demo data or destructive duplicate cleanup.
ALTER TABLE nanny_profiles
ADD UNIQUE INDEX IF NOT EXISTS uk_nanny_user_id (user_id);

ALTER TABLE payments
ADD UNIQUE INDEX IF NOT EXISTS uk_payment_booking_id (booking_id);

ALTER TABLE reviews
ADD UNIQUE INDEX IF NOT EXISTS uk_review_booking_reviewer (booking_id, reviewer_id);

ALTER TABLE bookings
ADD INDEX IF NOT EXISTS idx_bookings_parent (parent_id),
ADD INDEX IF NOT EXISTS idx_bookings_nanny (nanny_id),
ADD INDEX IF NOT EXISTS idx_bookings_status (status);

ALTER TABLE notifications
ADD INDEX IF NOT EXISTS idx_notifications_user_read (user_id, is_read);

ALTER TABLE chat_messages
ADD INDEX IF NOT EXISTS idx_messages_receiver_read (receiver_id, is_read);

ALTER TABLE parent_profiles
ADD INDEX IF NOT EXISTS idx_parent_user_id (user_id);

ALTER TABLE nanny_profiles
ADD INDEX IF NOT EXISTS idx_nanny_verification (verification_status),
ADD INDEX IF NOT EXISTS idx_nanny_rating (average_rating);
