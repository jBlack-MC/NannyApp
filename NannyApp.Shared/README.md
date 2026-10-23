# Shared backend services

Required by both PHP frontends. This directory owns database connections, migrations, authentication/security helpers, storage and shared operational services. It is not an optional standalone fallback.

## Database

The website and mobile API use one database: MariaDB for local Docker development or PostgreSQL/Supabase for the hosted application. Android accesses the PHP API, never database credentials or a privileged Supabase key.

From the repository root, with explicit target configuration:

```sh
php NannyApp.Shared/bin/migrate.php
```

The runner records checksums in `schema_migrations`, applies ordered shared migrations through v14, and applies native PostgreSQL SQL after the shared schema. [Migration order](database/MIGRATION_ORDER.md) and [PostgreSQL notes](database/postgres/README.md) describe locking, adoption and access restrictions. The Python Supabase runner is schema-only by default; seeding and password reset are separate explicit options.

Current `schema.sql` is empty idempotent DDL. Historical versions were destructive: do not import old dumps into an existing database. Do not edit an applied migration to change its checksum; add a migration.

## Authentication

Web sessions and API tokens refer to shared `users` records. Supabase Auth is not used. Private hosted demo credentials are distributed separately; `Password123!` is only for local development fixtures. [Account operations](../docs/DEMO_ACCOUNTS.md).

## Storage and workers

| Driver | Use |
| --- | --- |
| `local` | Persistent single-host disk, under `storage/uploads/` |
| `database` | Small stateless-host prototype; requires v12 |
| `s3` | Private S3-compatible object storage for larger deployments |

Select `NANNYAPP_STORAGE_DRIVER` and the corresponding environment settings in the [deployment guide](../docs/VERCEL_SUPABASE_DEPLOY.md). Shared files must not be directly public. Media endpoints authorize access before streaming or signing; public-CDN bypass is not supported. Database storage has capacity limits and is not a general replacement for object storage.

Email, notifications and file-deletion queues require workers. See [operations](../docs/PHASE1_OPERATIONS.md) and [Resend setup](../docs/RESEND_INTEGRATION.md). Ledger release/refund decisions do not perform bank transfers.

## Validation and releases

Run both `python tests/docker-checks.py` and `python tests/docker-checks.py --postgres` from the root. PostgreSQL tests cover schema replay, checksum drift, RLS/grants and shared web/API journeys. [CI/CD](../docs/CI_CD.md) explains production migration gates. [Android release files](releases/README.md) belong outside Git.
