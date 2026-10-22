# Supabase-specific migrations

These files are PostgreSQL-only and are applied separately from the shared
PHP/Python migration runners. Do not add them to the MariaDB migration list or
edit the checksums of already-applied shared migrations.

After shared migrations through v12, apply these files in order using Supabase's
versioned migration tooling, each in one transaction:

1. `20261022_backend_access_and_integrity.sql`
2. `20261022_sequences_and_duplicate_indexes.sql`

Both were applied to project `gumltynuuwwfstusfdzy` on 22 October 2026 using
Supabase's migration API, with names `backend_access_and_integrity` and
`sequences_and_duplicate_indexes`. The first file adds named constraints and
must not be applied twice. The Supabase migration history records application.

The application uses PHP/PDO with its own users, sessions and bearer tokens.
It does not use Supabase Auth identities or direct client table access.
RLS has no client policies intentionally: `anon` and `authenticated` have no
table or sequence privileges. PHP currently connects as table owner `postgres`,
which bypasses RLS. Do not enable FORCE ROW LEVEL SECURITY or switch the backend
to a different database role without preparing and testing its privileges.
The service role remains privileged; keep its credentials server-side.

Default privileges now deny direct client access to future public tables and
sequences created by postgres. New table migrations must also explicitly enable
RLS. Objects created by other roles need their own default-privilege review.

For verification, run `tests/security/supabase-hardening.sql` in Supabase SQL
Editor or through a trusted SQL client. It performs read-only checks and tests
that both direct-client roles are denied access to every application table.
Also verify `/api/health.php` and authenticated API requests after deployment.

Duplicate-index cleanup compares the complete PostgreSQL index definitions and
refuses to remove an index backing a constraint. No CASCADE or data cleanup is
used. No user, booking, payment, review, or uploaded-file rows are changed.
