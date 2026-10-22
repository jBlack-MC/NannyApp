-- PostgreSQL / Supabase only. Apply after backend_access_and_integrity.
-- No CASCADE and no row deletion. Refuse index drift or constraint dependencies.
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '60s';
REVOKE ALL PRIVILEGES ON SEQUENCE public.admin_profiles_id_seq FROM anon, authenticated, PUBLIC;
REVOKE ALL PRIVILEGES ON SEQUENCE public.api_tokens_id_seq FROM anon, authenticated, PUBLIC;
REVOKE ALL PRIVILEGES ON SEQUENCE public.apk_download_events_id_seq FROM anon, authenticated, PUBLIC;
REVOKE ALL PRIVILEGES ON SEQUENCE public.availability_slots_id_seq FROM anon, authenticated, PUBLIC;
REVOKE ALL PRIVILEGES ON SEQUENCE public.bookings_id_seq FROM anon, authenticated, PUBLIC;
REVOKE ALL PRIVILEGES ON SEQUENCE public.chat_messages_id_seq FROM anon, authenticated, PUBLIC;
REVOKE ALL PRIVILEGES ON SEQUENCE public.children_id_seq FROM anon, authenticated, PUBLIC;
REVOKE ALL PRIVILEGES ON SEQUENCE public.email_verifications_id_seq FROM anon, authenticated, PUBLIC;
REVOKE ALL PRIVILEGES ON SEQUENCE public.nanny_availability_id_seq FROM anon, authenticated, PUBLIC;
REVOKE ALL PRIVILEGES ON SEQUENCE public.nanny_portfolio_id_seq FROM anon, authenticated, PUBLIC;
REVOKE ALL PRIVILEGES ON SEQUENCE public.nanny_profiles_id_seq FROM anon, authenticated, PUBLIC;
REVOKE ALL PRIVILEGES ON SEQUENCE public.notification_outbox_id_seq FROM anon, authenticated, PUBLIC;
REVOKE ALL PRIVILEGES ON SEQUENCE public.notifications_id_seq FROM anon, authenticated, PUBLIC;
REVOKE ALL PRIVILEGES ON SEQUENCE public.page_content_id_seq FROM anon, authenticated, PUBLIC;
REVOKE ALL PRIVILEGES ON SEQUENCE public.parent_profiles_id_seq FROM anon, authenticated, PUBLIC;
REVOKE ALL PRIVILEGES ON SEQUENCE public.password_resets_id_seq FROM anon, authenticated, PUBLIC;
REVOKE ALL PRIVILEGES ON SEQUENCE public.payments_id_seq FROM anon, authenticated, PUBLIC;
REVOKE ALL PRIVILEGES ON SEQUENCE public.project_deliverables_id_seq FROM anon, authenticated, PUBLIC;
REVOKE ALL PRIVILEGES ON SEQUENCE public.reviews_id_seq FROM anon, authenticated, PUBLIC;
REVOKE ALL PRIVILEGES ON SEQUENCE public.saved_nannies_id_seq FROM anon, authenticated, PUBLIC;
REVOKE ALL PRIVILEGES ON SEQUENCE public.storage_deletions_id_seq FROM anon, authenticated, PUBLIC;
REVOKE ALL PRIVILEGES ON SEQUENCE public.subscriptions_id_seq FROM anon, authenticated, PUBLIC;
REVOKE ALL PRIVILEGES ON SEQUENCE public.support_tickets_id_seq FROM anon, authenticated, PUBLIC;
REVOKE ALL PRIVILEGES ON SEQUENCE public.users_id_seq FROM anon, authenticated, PUBLIC;
DO $cleanup$
DECLARE pair record;
BEGIN
  FOR pair IN SELECT * FROM (VALUES
    ('idx_b_nanny','idx_bookings_nanny'),
    ('idx_b_parent','idx_bookings_parent'),
    ('idx_b_status','idx_bookings_status'),
    ('idx_cm_recv_read','idx_messages_receiver_read'),
    ('idx_np_rating','idx_nanny_rating'),
    ('idx_np_verif','idx_nanny_verification'),
    ('uq_nanny_user','uk_nanny_user_id'),
    ('idx_n_user_read','idx_notifications_user_read'),
    ('uq_pay_booking','uk_payment_booking_id'),
    ('uq_rev_booking','uk_review_booking_reviewer')
  ) AS candidates(old_name, keep_name)
  LOOP
    IF to_regclass('public.' || pair.old_name) IS NULL THEN CONTINUE; END IF;
    IF NOT EXISTS (
      SELECT 1 FROM pg_index old_idx JOIN pg_index keep_idx
        ON (to_jsonb(old_idx) - 'indexrelid') = (to_jsonb(keep_idx) - 'indexrelid')
      WHERE old_idx.indexrelid=to_regclass('public.' || pair.old_name)
        AND keep_idx.indexrelid=to_regclass('public.' || pair.keep_name)
        AND keep_idx.indisvalid AND keep_idx.indisready
    ) THEN
      RAISE EXCEPTION 'Index definitions differ: % / %', pair.old_name, pair.keep_name;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conindid=to_regclass('public.' || pair.old_name)) THEN
      RAISE EXCEPTION 'Constraint depends on %', pair.old_name;
    END IF;
    EXECUTE format('DROP INDEX public.%I', pair.old_name);
  END LOOP;
END
$cleanup$;

