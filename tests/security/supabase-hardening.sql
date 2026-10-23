-- Read-only post-deployment regression checks for this Supabase project.
BEGIN READ ONLY;
DO $verify$
DECLARE
  table_name text;
  client_role text;
BEGIN
  FOREACH table_name IN ARRAY ARRAY['contact_messages','admin_profiles','api_tokens','apk_download_events','availability_slots','bookings','chat_messages','children','email_outbox','email_verifications','nanny_availability','nanny_portfolio','nanny_profiles','notification_outbox','notifications','page_content','parent_profiles','password_resets','payments','profile_views','project_deliverables','public_holidays_cache','reviews','saved_nannies','schema_migrations','security_rate_limits','storage_deletions','stored_media','subscriptions','support_tickets','translation_cache','users','web_sessions'] LOOP
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE oid=to_regclass('public.'||table_name) AND relrowsecurity) THEN
      RAISE EXCEPTION 'RLS missing: %', table_name;
    END IF;
    FOREACH client_role IN ARRAY ARRAY['anon','authenticated'] LOOP
      IF has_table_privilege(client_role, 'public.'||table_name, 'SELECT,INSERT,UPDATE,DELETE,TRUNCATE,REFERENCES,TRIGGER') THEN
        RAISE EXCEPTION 'Unexpected table privilege: % on %', client_role, table_name;
      END IF;
      -- Exercise PostgreSQL access enforcement without reading any row data.
      BEGIN
        EXECUTE format('SET LOCAL ROLE %I', client_role);
        EXECUTE format('SELECT 1 FROM public.%I LIMIT 0', table_name);
        RAISE EXCEPTION 'Direct client access unexpectedly succeeded: % on %', client_role, table_name;
      EXCEPTION WHEN insufficient_privilege THEN
        NULL; -- This subtransaction also restores the original role.
      END;
    END LOOP;
  END LOOP;
  IF (SELECT count(*) FROM pg_constraint WHERE connamespace='public'::regnamespace AND convalidated
      AND conname IN ('ck_users_role','ck_users_status','ck_users_email_verified','ck_bookings_duration_positive','ck_payments_amount_nonnegative','ck_nanny_rate_nonnegative')) <> 6 THEN
    RAISE EXCEPTION 'Missing or unvalidated data checks';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_index WHERE indexrelid=to_regclass('public.uq_saved_parent_nanny') AND indisunique AND indisvalid)
     OR NOT EXISTS (SELECT 1 FROM pg_index WHERE indexrelid=to_regclass('public.uq_users_normalized_email') AND indisunique AND indisvalid) THEN
    RAISE EXCEPTION 'Missing uniqueness guards';
  END IF;
END
$verify$;
COMMIT;

