-- PostgreSQL / Supabase only. Apply after the shared migrations through v12.
-- The PHP backend connects as postgres; clients never query these tables directly.
-- No row data is changed. Run the entire file in one transaction.
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '60s';
ALTER TABLE public.admin_profiles ENABLE ROW LEVEL SECURITY;
REVOKE ALL PRIVILEGES ON TABLE public.admin_profiles FROM anon, authenticated, PUBLIC;
ALTER TABLE public.api_tokens ENABLE ROW LEVEL SECURITY;
REVOKE ALL PRIVILEGES ON TABLE public.api_tokens FROM anon, authenticated, PUBLIC;
ALTER TABLE public.apk_download_events ENABLE ROW LEVEL SECURITY;
REVOKE ALL PRIVILEGES ON TABLE public.apk_download_events FROM anon, authenticated, PUBLIC;
ALTER TABLE public.availability_slots ENABLE ROW LEVEL SECURITY;
REVOKE ALL PRIVILEGES ON TABLE public.availability_slots FROM anon, authenticated, PUBLIC;
ALTER TABLE public.bookings ENABLE ROW LEVEL SECURITY;
REVOKE ALL PRIVILEGES ON TABLE public.bookings FROM anon, authenticated, PUBLIC;
ALTER TABLE public.chat_messages ENABLE ROW LEVEL SECURITY;
REVOKE ALL PRIVILEGES ON TABLE public.chat_messages FROM anon, authenticated, PUBLIC;
ALTER TABLE public.children ENABLE ROW LEVEL SECURITY;
REVOKE ALL PRIVILEGES ON TABLE public.children FROM anon, authenticated, PUBLIC;
ALTER TABLE public.email_outbox ENABLE ROW LEVEL SECURITY;
REVOKE ALL PRIVILEGES ON TABLE public.email_outbox FROM anon, authenticated, PUBLIC;
ALTER TABLE public.email_verifications ENABLE ROW LEVEL SECURITY;
REVOKE ALL PRIVILEGES ON TABLE public.email_verifications FROM anon, authenticated, PUBLIC;
ALTER TABLE public.nanny_availability ENABLE ROW LEVEL SECURITY;
REVOKE ALL PRIVILEGES ON TABLE public.nanny_availability FROM anon, authenticated, PUBLIC;
ALTER TABLE public.nanny_portfolio ENABLE ROW LEVEL SECURITY;
REVOKE ALL PRIVILEGES ON TABLE public.nanny_portfolio FROM anon, authenticated, PUBLIC;
ALTER TABLE public.nanny_profiles ENABLE ROW LEVEL SECURITY;
REVOKE ALL PRIVILEGES ON TABLE public.nanny_profiles FROM anon, authenticated, PUBLIC;
ALTER TABLE public.notification_outbox ENABLE ROW LEVEL SECURITY;
REVOKE ALL PRIVILEGES ON TABLE public.notification_outbox FROM anon, authenticated, PUBLIC;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;
REVOKE ALL PRIVILEGES ON TABLE public.notifications FROM anon, authenticated, PUBLIC;
ALTER TABLE public.page_content ENABLE ROW LEVEL SECURITY;
REVOKE ALL PRIVILEGES ON TABLE public.page_content FROM anon, authenticated, PUBLIC;
ALTER TABLE public.parent_profiles ENABLE ROW LEVEL SECURITY;
REVOKE ALL PRIVILEGES ON TABLE public.parent_profiles FROM anon, authenticated, PUBLIC;
ALTER TABLE public.password_resets ENABLE ROW LEVEL SECURITY;
REVOKE ALL PRIVILEGES ON TABLE public.password_resets FROM anon, authenticated, PUBLIC;
ALTER TABLE public.payments ENABLE ROW LEVEL SECURITY;
REVOKE ALL PRIVILEGES ON TABLE public.payments FROM anon, authenticated, PUBLIC;
ALTER TABLE public.profile_views ENABLE ROW LEVEL SECURITY;
REVOKE ALL PRIVILEGES ON TABLE public.profile_views FROM anon, authenticated, PUBLIC;
ALTER TABLE public.project_deliverables ENABLE ROW LEVEL SECURITY;
REVOKE ALL PRIVILEGES ON TABLE public.project_deliverables FROM anon, authenticated, PUBLIC;
ALTER TABLE public.public_holidays_cache ENABLE ROW LEVEL SECURITY;
REVOKE ALL PRIVILEGES ON TABLE public.public_holidays_cache FROM anon, authenticated, PUBLIC;
ALTER TABLE public.reviews ENABLE ROW LEVEL SECURITY;
REVOKE ALL PRIVILEGES ON TABLE public.reviews FROM anon, authenticated, PUBLIC;
ALTER TABLE public.saved_nannies ENABLE ROW LEVEL SECURITY;
REVOKE ALL PRIVILEGES ON TABLE public.saved_nannies FROM anon, authenticated, PUBLIC;
ALTER TABLE public.schema_migrations ENABLE ROW LEVEL SECURITY;
REVOKE ALL PRIVILEGES ON TABLE public.schema_migrations FROM anon, authenticated, PUBLIC;
ALTER TABLE public.security_rate_limits ENABLE ROW LEVEL SECURITY;
REVOKE ALL PRIVILEGES ON TABLE public.security_rate_limits FROM anon, authenticated, PUBLIC;
ALTER TABLE public.storage_deletions ENABLE ROW LEVEL SECURITY;
REVOKE ALL PRIVILEGES ON TABLE public.storage_deletions FROM anon, authenticated, PUBLIC;
ALTER TABLE public.stored_media ENABLE ROW LEVEL SECURITY;
REVOKE ALL PRIVILEGES ON TABLE public.stored_media FROM anon, authenticated, PUBLIC;
ALTER TABLE public.subscriptions ENABLE ROW LEVEL SECURITY;
REVOKE ALL PRIVILEGES ON TABLE public.subscriptions FROM anon, authenticated, PUBLIC;
ALTER TABLE public.support_tickets ENABLE ROW LEVEL SECURITY;
REVOKE ALL PRIVILEGES ON TABLE public.support_tickets FROM anon, authenticated, PUBLIC;
ALTER TABLE public.translation_cache ENABLE ROW LEVEL SECURITY;
REVOKE ALL PRIVILEGES ON TABLE public.translation_cache FROM anon, authenticated, PUBLIC;
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
REVOKE ALL PRIVILEGES ON TABLE public.users FROM anon, authenticated, PUBLIC;
ALTER TABLE public.web_sessions ENABLE ROW LEVEL SECURITY;
REVOKE ALL PRIVILEGES ON TABLE public.web_sessions FROM anon, authenticated, PUBLIC;
-- Prevent new postgres-owned tables and sequences inheriting public API grants.
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public REVOKE ALL ON TABLES FROM anon, authenticated, PUBLIC;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public REVOKE ALL ON SEQUENCES FROM anon, authenticated, PUBLIC;

-- Cover the foreign keys omitted during the original MySQL-to-Postgres translation.
CREATE INDEX IF NOT EXISTS idx_apitoken_user ON public.api_tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_slot_nanny ON public.availability_slots (nanny_id);
CREATE INDEX IF NOT EXISTS idx_messages_sender ON public.chat_messages (sender_id);
CREATE INDEX IF NOT EXISTS idx_child_parent ON public.children (parent_id);
CREATE INDEX IF NOT EXISTS idx_verify_user ON public.email_verifications (user_id);
CREATE INDEX IF NOT EXISTS idx_avail_nanny ON public.nanny_availability (nanny_id);
CREATE INDEX IF NOT EXISTS idx_port_nanny ON public.nanny_portfolio (nanny_id);
CREATE INDEX IF NOT EXISTS idx_pc_admin ON public.page_content (updated_by);
CREATE INDEX IF NOT EXISTS idx_reset_user ON public.password_resets (user_id);
CREATE INDEX IF NOT EXISTS idx_profile_view_viewer ON public.profile_views (viewer_id);
CREATE INDEX IF NOT EXISTS idx_review_reviewer ON public.reviews (reviewer_id);
CREATE INDEX IF NOT EXISTS idx_save_nanny ON public.saved_nannies (nanny_id);
CREATE INDEX IF NOT EXISTS idx_subscription_user ON public.subscriptions (user_id);
-- This unique index also covers saved_nannies.parent_id and makes INSERT IGNORE effective.
CREATE UNIQUE INDEX IF NOT EXISTS uq_saved_parent_nanny ON public.saved_nannies (parent_id, nanny_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_normalized_email ON public.users (lower(btrim(email)));

-- Reintroduce basic domain checks lost when MySQL ENUM/UNSIGNED became VARCHAR/INTEGER.
ALTER TABLE public.users ADD CONSTRAINT ck_users_role CHECK (role IN ('parent','nanny','admin'));
ALTER TABLE public.users ADD CONSTRAINT ck_users_status CHECK (status IN ('active','suspended'));
ALTER TABLE public.users ADD CONSTRAINT ck_users_email_verified CHECK (email_verified IN (0,1));
ALTER TABLE public.bookings ADD CONSTRAINT ck_bookings_duration_positive CHECK (duration > 0);
ALTER TABLE public.payments ADD CONSTRAINT ck_payments_amount_nonnegative CHECK (amount >= 0);
ALTER TABLE public.nanny_profiles ADD CONSTRAINT ck_nanny_rate_nonnegative CHECK (hourly_rate >= 0);

