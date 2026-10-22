# Live database review — 22 October 2026

Project: `gumltynuuwwfstusfdzy` (Supabase PostgreSQL).

## Applied improvements

- Enabled RLS on all 32 application tables and revoked direct table access from
  `anon`, `authenticated` and PUBLIC. The PHP backend retains its owner access.
- Revoked direct-client access to the 24 application identity sequences and
  tightened defaults for future postgres-owned public tables and sequences.
- Added 14 indexes covering all previously unindexed foreign keys. The saved
  nanny index also enforces uniqueness of `(parent_id, nanny_id)`.
- Added a unique normalized-email index to prevent case/whitespace variants
  from creating duplicate identities.
- Added six validated checks: user role, user status, email verification flag,
  positive booking duration, nonnegative payment amount and nanny hourly rate.
- Removed ten exactly duplicate indexes after checking identical definitions,
  replacement validity and absence of constraint dependencies.

Migration sources and replay instructions:
[PostgreSQL migrations](../NannyApp.Shared/database/postgres/README.md).

## Evidence

The integrity review found 38 users, 10 bookings, 9 reviews and no payment rows.
There were no duplicate normalized emails or saved pairs, invalid roles/statuses/
verification flags, nonpositive durations, negative payments/rates, incorrect
parent/nanny booking roles or mismatched booking/review participants. All existing
foreign-key constraints were validated. Application record counts were unchanged
by the migrations; this was schema/access maintenance, not data cleanup.

After migration, the Supabase advisors report no security errors, missing
foreign-key indexes or duplicate-index warnings. Remaining informational notices
are RLS with no client policies (intentional for this backend-only architecture)
and unused indexes on the small dataset. Index usage alone is not grounds for
removing an index needed for constraints or future queries.

[RLS advisory reference](https://supabase.com/docs/guides/database/database-linter?lint=0013_rls_disabled_in_public)
and [foreign-key index reference](https://supabase.com/docs/guides/database/database-linter?lint=0001_unindexed_foreign_keys).

Read-only access regressions in
[`supabase-hardening.sql`](../tests/security/supabase-hardening.sql) passed,
including actual denied SELECT attempts as both client roles against all 32 tables.
No direct-client sequence permissions remain. Live health checks pass for the
database, notifications, email and storage. Parent and nanny API login and
authenticated profile retrieval still return HTTP 200.

## Outstanding work

The website dashboard still returns HTTP 500 after successful authentication.
The separately prepared PHP query-portability fix needs Vercel deployment; these
database changes do not deploy PHP code. No APK device test was performed.

This review did not establish whether earlier direct table access was exploited,
perform a backup/restore drill, or replace the privileged backend connection with
a dedicated least-privilege database role. Those remain separate follow-up work.
