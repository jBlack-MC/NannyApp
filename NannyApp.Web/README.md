# Nanny-App website

PHP website and browser administration for the shared childcare marketplace. Supports MariaDB and Supabase PostgreSQL through the shared PDO layer. No Composer or frontend bundler is required for the PHP application. The PWA is the website; the distributed native Android app lives in `NannyApp.Mobile`.

## Run and configure

Follow [local development](../docs/LOCAL_DEVELOPMENT.md) from the repository root, keeping `NannyApp.Shared` alongside this directory. For hosting, use [Vercel + Supabase](../docs/VERCEL_SUPABASE_DEPLOY.md) or the [AWS alternative](../docs/AWS_DEPLOYMENT.md). Use environment configuration for database, base URL, email and storage; never put production secrets into tracked PHP files.

Run `php NannyApp.Shared/bin/migrate.php` from the repository root with the target database explicitly configured. The versioned runner includes the contact inbox in v14 and applies PostgreSQL-specific restrictions. See [migration order](../NannyApp.Shared/database/MIGRATION_ORDER.md).

## Accounts and shared data

[Demo accounts and access instructions](../README.md#sign-in-and-demo-accounts) are maintained in the root README. `Password123!` applies only to local fixtures. Hosted accounts require their issued private passwords. Administration is restricted to a normal browser; mobile API admin login is intentionally refused.

Web sessions and Android bearer tokens use the same user records and shared authorization rules. Registration in either client creates an account visible to the other. Verification and account status still apply. Password changes and logout revoke prior sessions; clients discover revocation on their next request.

## Main routes

| Directory or route | Purpose |
| --- | --- |
| `auth/` | Login, registration, verification and recovery |
| `parent/` | Children, nanny search, bookings and manual payment records |
| `nanny/` | Profile, availability, bookings and earnings ledger |
| `admin/` | Verification, users, support, contacts and reports |
| `admin/operations.php` | Outstanding work, delivery queues and system health |
| `media.php` | Authorized access to shared uploads |
| `download-app.php` | Current signed native Android APK download |

The operations page requires migrated contact and queue tables. An error reference belongs in the server logs; use [troubleshooting](../docs/TROUBLESHOOTING.md) and apply missing migrations. Queue counts do not prove that a worker is healthy. Payment statuses record manual decisions and do not move money.

## Caching and verification

Public assets/content can use the service worker. Login, registration, authenticated pages and private API responses must remain uncached. Do not add a CDN cache rule over authentication routes to fix login issues.

Run `python tests/docker-checks.py` and `python tests/docker-checks.py --postgres` from the root. Both engines have real HTTP integration coverage; PostgreSQL covers parent/nanny signup in both clients and authenticated admin routes. [CI/CD](../docs/CI_CD.md) describes checks and deployment verification. Physical-device and external-provider checks remain separate.
