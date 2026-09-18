# Database migration order

`schema.sql` creates a local development database, including demo accounts. It
starts with `DROP DATABASE`, so it must **never** be run against staging or
production.

For a new local database:

1. Import `schema.sql`.
2. Run `migrate_v2.sql`.
3. Run `migrate_v3.sql`.
4. Run `migrate_v4.sql`.
5. Run `migrate_v5_api.sql` for the native API bearer-token table.
6. Apply `phase1_constraints.sql` and `phase2_authentication.sql` only after
   reviewing their notes and taking a backup.

Before every non-local migration: back up the database, apply the migration to
staging first, verify parent/nanny/admin flows, then record the filename and
deployment date in the release notes. This repository does not yet include an
automated migration runner or database-version table; add one before frequent
production releases.
