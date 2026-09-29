# Safe database setup and migrations

Use MariaDB 10.11 (the tested runtime). Create an empty database and migration account in the host panel; the runner never creates, selects a hard-coded database, drops or clears an application database. Back up an existing database and test its upgrade on a disposable copy first.

Set `NANNYAPP_DB_HOST`, `NANNYAPP_DB_PORT`, `NANNYAPP_DB_NAME`, `NANNYAPP_DB_USER` and `NANNYAPP_DB_PASS` in the process environment, then from the repository root run:

```sh
php NannyApp.Shared/bin/migrate.php
```

The database name and user must be explicit. This command is CLI-only. Historical web migration URLs now return 404. `.env.production.example` is a reference, not an automatically loaded dotenv file.

The order is schema, v2, v3, v4, v5, phase1 constraints, phase2 authentication, v6 security, v7 email outbox, v8 operational queues. `schema.sql` is now an idempotent empty schema, with no DROP DATABASE, database selection or demo accounts. Older revisions of this file were destructive: do not use downloaded old copies. Demo seed scripts are not part of setup and must not be shipped to production.

A database advisory lock prevents concurrent migration runners. `schema_migrations` stores filenames and normalized checksums; a changed applied migration is rejected. Future changes require a new migration file. Existing installations without a migration table are adopted through idempotent statements. Unique-index conflicts fail without deleting duplicates; review and resolve conflicting records separately, then retry. No ALTER IGNORE or silent data repair is performed. Historical demo verification/admin backfills and marketing copy inserts have been removed.

MariaDB DDL auto-commits. An interrupted upgrade can leave some additive changes applied; it does not pretend to roll back DDL. Resolve the reported failure and rerun the same files. Take a backup before schema changes, including enum expansion. Schema drift and missing legacy columns can require manual investigation; do not change checksums to bypass an error.

Bootstrap creates no admin or other accounts. Register an owner account and grant the intended administrative access through a separately authorized administrative procedure before a pilot. Keep migration credentials separate from the application's data-only user.

Validation: `tests/security/migrations.test.php` uses randomly named disposable schemas to exercise fresh setup, populated repeats, legacy adoption, checksum refusal and conflicting-data preservation. These tests do not replace a restore drill for a real hosting account.
