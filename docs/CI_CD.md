# CI/CD and release operations

This is the executable pipeline map for the web application, PHP API, both database engines and Android. Workflow files are authoritative. PR checks use synthetic local data; production workflows are separate.

## Continuous integration

`ci.yml` runs on PRs, pushes to main, manual dispatch and weekly. Feature-branch pushes use the PR run instead of duplicating the full Android/database suite. Existing required check names are preserved:

| Check | Work |
| --- | --- |
| PHP and synthetic security tests | Source-only Docker image, MariaDB, PHP/JS validation, HTTP auth, ownership, bookings, mail, uploads and caching |
| PostgreSQL schema and five-account seed | Real PostgreSQL migrations, seed preservation, checksum drift, native migration adoption, RLS/grants and cross-client registration/data |
| Android build, unit tests and lint | Debug APK, JVM tests, lint, required-release-URL negative test and unsigned release compilation |
| Validate Task 2 deliverables | README local links/credential-pattern checks and HTML/PDF catalogue validation |

Android verification artifacts expire after seven days. They are not signed production releases. Physical-device acceptance, live mail delivery and backup restores remain separate tasks. CodeQL is configured through GitHub's repository integration; inspect its checks in the PR rather than adding a second duplicate scan.

`workflow-checks.yml` runs actionlint on every PR and main push, plus manual/weekly runs. `production-verify.yml` runs after a successful production deployment event or a successful Vercel status for main, manually on main, and daily. The status trigger covers the current integration, which reports commit statuses rather than GitHub deployment records. It reads the configured production origin only; it does not trust a URL supplied by the deployment event or execute its commit. GitHub describes this event in its [workflow event reference](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows#deployment_status).

The read-only production check requires HTTP 200 for the homepage, login, registration and healthy API; HTTP 401 for anonymous profile access; and `no-store` on auth/API responses. It does not create accounts, verify passwords, prove a particular deployment SHA, or check authenticated admin routes. A green public health check alone cannot prove every migrated table exists.

## Configuration

| Setting | Location and use |
| --- | --- |
| `NANNYAPP_PRODUCTION_URL` | GitHub variable: HTTPS website origin, currently `https://nannyapp-two.vercel.app` |
| `NANNYAPP_RELEASE_API_URL` | GitHub variable: same origin plus `/api/` |
| `FREE_CI_CONFIRMED` | Private repositories only: owner confirmation after checking Actions spending settings |
| `NANNYAPP_DB_URL` | Optional GitHub secret for direct PostgreSQL schema migration |
| `NANNYAPP_MIGRATION_TOKEN` | Matching GitHub and deployed application secret for protected HTTP migration fallback |
| `NANNYAPP_DEMO_PASSWORD` | GitHub secret used only for explicit demo creation/rotation; not a public login example |
| `NANNYAPP_RELEASE_KEYSTORE_BASE64` | GitHub signing secret |
| `NANNYAPP_RELEASE_KEYSTORE_PASSWORD`, `NANNYAPP_RELEASE_KEY_ALIAS`, `NANNYAPP_RELEASE_KEY_PASSWORD` | GitHub signing secrets |

Database credentials belong in Vercel's server environment as described in [deployment setup](VERCEL_SUPABASE_DEPLOY.md). Do not put them in client-visible variables or README examples. Production database jobs use `production-db`; signing uses `production-release`. Configure required reviewers and branch restrictions in GitHub environments as needed. Never run production secrets in fork/PR tests.

## Web/API and database release

1. Open a PR and pass required checks/review. Merge the reviewed commit into main.
2. Use the connected Vercel Git integration for web/API deployment. Preview success is not production success. Confirm the production deployment in Vercel/GitHub; this repository does not have an independently configured Vercel CLI deployment token.
3. Apply additive schema changes before code depends on them when direct database access is available. Dispatch **Supabase database sync** from main; its default is schema-only. With the HTTP fallback, the deployed migration endpoint must contain the new migration first. Use a maintenance window when that ordering would temporarily break requests.
4. Leave `seed_demo` and `reset_demo_passwords` false for ordinary deployments. Reset requires both explicit inputs and direct DB access, and revokes demo sessions. Do not repeatedly seed sample activity on production.
5. Check **Production verification**, then verify authenticated parent/nanny/admin pages and Android against the deployed API. The operations and contacts pages need shared v14. Record the actual deployed revision, migration result and device result.

The screenshot failure on 23 October 2026 was caused by the absent `contact_messages` table. The additive v14 repair restored both live admin pages to HTTP 200. The normal migration runner retains its checksum; future deploys should use the complete versioned sequence rather than ad hoc table creation in page requests.

## Android release

1. Increase versionCode/versionName, test the actual backend on a physical device and merge the change.
2. Create an immutable `vX.Y.Z` tag on that reviewed main commit.
3. Dispatch **Android release** from main with that existing tag. The workflow checks ancestry, builds with the configured API URL, verifies the embedded endpoint and signature, then publishes the release asset.
4. Verify the website's download resolves to the intended signed APK. Record its actual binary version, SHA-256 and install/upgrade result. An unsigned CI APK or old `app/release/` file is not a substitute.

## Failure and rollback

Read the failed job first; do not rerun a credential rotation to fix a schema or deployment error. Use [troubleshooting](TROUBLESHOOTING.md) for application error references. Roll back web/API code through the hosting provider to a known compatible revision. Keep additive database migrations in place unless a separately reviewed reversal is necessary; never drop production data to make tests pass. Preserve the prior signed APK and signing key. Android rollback installation may require a new higher version code.

Automatic merge is currently disabled and normal main merges require a review. Pipeline configuration alone does not approve a release, configure a missing hosting integration, guarantee available runner quota, or prove restore readiness.
