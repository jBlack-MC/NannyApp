SET LOCAL lock_timeout = '5s';
ALTER TABLE public.contact_messages ENABLE ROW LEVEL SECURITY;
REVOKE ALL PRIVILEGES ON TABLE public.contact_messages FROM anon, authenticated, PUBLIC;
REVOKE ALL PRIVILEGES ON SEQUENCE public.contact_messages_id_seq FROM anon, authenticated, PUBLIC;
