# Security regressions

## Automated execution

`python tests/docker-checks.py` builds a filtered source-only test image, runs the full Linux suite with disposable MariaDB and cleans up the test stack. CI uses this same entry point. `python tests/run-checks.py --php /path/to/php` runs checks without Docker; add `--backend` only with a dedicated test database. Details: [Automation](../../docs/AUTOMATION.md).

The Apache test now supports Debian Apache as well as Windows XAMPP. Backend coverage also includes review-target authorization, rating validation and completed-booking ownership.

Only synthetic users, documents, tokens and email addresses are used. No test
imports `schema.sql` or connects to the application's configured database.

## Backend and HTTP tests

Start a **disposable** local MariaDB server on port 13379 with an empty root
password and no external network access. The suite refuses the normal 3306 port,
creates a randomly named `nanny_security_test_*` schema and drops only that schema
in `finally`. `NANNYAPP_TEST_DB_PORT` can select another dedicated local port.
Port 13381 must be free for the PHP test HTTP server.

```text
python tests/security/run-backend.py /path/to/php
```

Requires PHP 8.1+ with PDO MySQL and Python 3. The wrapper starts an ephemeral
loopback SMTP sink and captures messages in memory, accepting only
`@example.invalid` recipients. Unix PHP uses the bundled synthetic sendmail
adapter. Run from the repository root. Without the SMTP wrapper,
`php tests/security/backend.php` runs all checks except successful delivery.

Coverage: owner/admin/unrelated/anonymous local document access; authorization
before S3 signing and public-CDN bypass prevention; pending-registration tokens
on real protected endpoints; signup/resend/recovery delivery and failure/retry;
verification/reset expiry and reuse; active web session and bearer revocation;
ordinary API/web password changes, old-session/reset-link revocation, stale-login
rejection, invalid password payloads and profile-write rollback; automatic payment-release eligibility, competing workers and injected ledger-write rollback; parallel password
changes and single-use reset consumption and rollback; cookieless, session-rotation,
expiry, failure-closed and independent-process atomic rate limiting.

S3 signing uses synthetic keys and an `.invalid` endpoint. No bucket is contacted.
External mail deliverability, hosted bucket ACLs and proxy configuration remain
deployment checks.

## PWA, Apache and Android

```text
node tests/security/service-worker.test.cjs
python tests/security/android-cache.test.py
python tests/security/apache.test.py C:/xampp/apache
```

The PWA tests execute the actual worker with simulated network/cache events.
The SQLite tests execute the actual Room DAO query strings on synthetic rows.
The Apache test uses a temporary document root, copied application deny rules,
synthetic logs and loopback port 13383; this runner currently targets Windows
Apache. No existing logs are opened or printed.

From `NannyApp.Mobile/NannyApp`:

```text
gradlew.bat --no-daemon :app:assembleDebug :app:testDebugUnitTest
```

`SessionCacheBoundaryTest` covers account switches, late responses, logout,
concurrent cache writes and rejection of HTTP-error fallback. These are JVM
tests; device navigation and the Room 1-to-2 upgrade still need device testing.

## Deployment requirements for these fixes

1. Apply `NannyApp.Shared/database/migrate_v6_security.sql` after the existing
   v3/v5/auth migrations, before deploying the PHP code. Tested with MariaDB
   10.4.32; its `ADD COLUMN IF NOT EXISTS` syntax needs adaptation/verification
   for other database engines. It preserves application data. Missing limiter
   storage blocks protected attempts rather than permitting them.
2. Deploy `NannyApp.Shared` alongside both clients. Shared security/mail helpers
   are required. API database settings can come from environment variables when
   the optional local credentials file is absent.
3. Set an absolute HTTPS `NANNYAPP_BASE_URL` for the web portal,
   `NANNYAPP_MAIL_TRANSPORT=mail`, and a valid `NANNYAPP_MAIL_FROM`. Configure the
   host PHP mail transport/MTA; application `SMTP_*` variables are not supported.
   Delivery is disabled unless explicitly configured. No mail bodies are logged.
4. Keep the S3 bucket private. The application ignores public-CDN shortcuts and
   signs authorized media requests for 60 seconds. Protect equivalent paths in
   non-Apache servers. Existing public object copies require deployment cleanup.
5. Deploy Apache deny rules and the new service worker. Existing web sessions
   without a password fingerprint require login again. Worker activation purges
   old `nannyapp-*` caches; logout also clears browser caches/storage.
6. Android Room v2 discards legacy unscoped private cache rows, preserves public
   profile/review data and adds account ownership to payment/saved/notification
   caches. Session changes clear Room data. Backup is disabled for private data.
7. Two historical mail logs are removed from Git's index but remain locally and
   in history. Assess historical exposure and revoke affected live tokens on
   deployed systems; history rewriting is deliberately not automated.

Periodically remove expired limiter rows using the maintenance statement in the
migration. All application instances must share the same database; client IP is
the trusted server `REMOTE_ADDR`, not a caller-provided forwarding header.

## App distribution checks

`node tests/security/install.test.cjs` covers install prompt acceptance, dismissal, browser failure and already-installed state without any provider.

`python tests/security/download.test.py /path/to/php` starts a disposable PHP server on loopback port 13386 and serves a temporary synthetic non-installable APK. It checks the public landing page, exact download bytes/headers and missing-release handling. Do not run with another server using that port. It does not create or publish a real APK.
