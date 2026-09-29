# Production readiness audit

## Automation update - 2026-09-28

- [x] Docker synthetic PHP/MariaDB/HTTP/SMTP/Resend, Apache, PWA/cache and install/download checks pass locally.
- [x] Android debug build, JVM tests and lint pass (0 errors, 52 warnings); WorkManager initializer conflict fixed.
- [x] Explicit release API URL required; missing URL fails and an unsigned synthetic-URL release compiles. Real signed release/device validation remains open.
- [x] Review target bound to the authorized completed booking; mismatched-target, invalid-rating, ownership/status, successful-rating and duplicate regressions pass.
- [x] Ordinary password changes atomically revoke bearer/remember sessions and reset links; concurrent stale logins cannot recreate tokens. Synthetic API/web/concurrency/rollback checks pass; Android debug build, 7 JVM tests and lint pass (0 errors, 52 warnings).
- [ ] Enable private-repository CI after verifying free-only billing, run remote jobs, and configure required checks if the repository plan supports them.

See [AUTOMATION.md](AUTOMATION.md). Public hosting, real mail/storage verification and outstanding booking security defects are not completed by these checks.

## Booking ledger update - 2026-09-28

- [x] Automatic completion and payment release share one transactional helper across web/API. Eligibility is rechecked during the write; ledger failures roll back completion.
- [x] Synthetic disputed/recent candidates, competing release workers and injected payment-write failure regressions pass in the full Docker suite.
- [ ] Booking creation validation, serialized overlap checks and all manual status/ledger transitions remain open (P08/P09). In particular, disputes raised after release still need an explicit ledger policy; notification delivery is not yet durable.

## GitHub Actions - 2026-09-29

- [x] Diagnosed existing remote failures: GitHub account billing lock prevents jobs starting; see [automation troubleshooting](AUTOMATION.md).
- [x] Added weekly CI and workflow-validation schedules; pinned Ubuntu 24.04 and retained free-only gates. Both workflow files pass local actionlint 1.7.12 validation.
- [x] Added read-only `tests/check-actions.py` and CI result summaries; diagnostic verified against the real failed run and updated YAML passes actionlint.
- [ ] Owner resolves GitHub account restriction without enabling paid overages, pushes updated workflows and verifies new runs. Local checks remain available.

## Budget constraint: free services only

Follow [FREE_DEPLOYMENT_PLAN.md](FREE_DEPLOYMENT_PLAN.md). AWS provisioning and paid store/domain enrollment are inactive. Use local tests, evaluate a free PHP/database host with its HTTPS subdomain, keep uploads private using the local storage driver, and distribute through direct Android download and the web app. Hosting/email/privacy compatibility remains unverified; this is not launch approval.

## Current status - 2026-09-28

- [x] Requested security remediation and synthetic backend/PWA/Android isolation tests implemented.
- [x] Optional Resend verification/recovery integration and synthetic failure tests implemented.
- [x] [Pilot deployment plan](PILOT_DEPLOYMENT.md) and [code/platform/resilience review](CODE_REVIEW_2026-09-27.md) delivered.
- [ ] Remaining review defects, real hosting/secrets/migrations, restore checks, provider delivery and browser/device acceptance.

Requirements below are launch gates, not completed checks. Use the pilot plan; permanent staging and S3 are not mandatory for this stage.

## Implemented project boundaries

- The Android app is for parents and nannies; administrator navigation is not exposed there.
- Web administration remains available through the PHP web portal.
- The native app uses the shared logo-only `assets/Icon_Logo.png` artwork and a mobile-safe Compose design system aligned to the web palette.
- The project now has a repeatable test home under `/tests` and an Android unit-test baseline.

## Required before a public online launch

These items include remaining source fixes and operational checks; source completion alone does not establish launch readiness.

1. **Hosting and domain:** configure production PHP/MySQL hosting, HTTPS certificates, DNS, firewall rules, backups, and a real Android `API_BASE_URL`. The release placeholder `https://your-domain.example.com/nannyapp/api/` must never ship.
2. **Secrets:** create environment-specific database, SMTP, session, and API credentials outside source control. Rotate any credential that was ever committed or shared.
3. **Email:** activate and verify the implemented Resend account-email integration (or configured PHP mail) for verification and recovery. Test real delivery and sender-domain alignment. Booking/support email dispatch is outside this integration.
4. **Uploads:** move identity documents and profile uploads off the public web root (or use private object storage), enforce MIME/size checks, and verify backup/restore.
5. **Payments:** keep launch copy and backend status manual/offline until a payment gateway, webhook signature verification, idempotency, refund policy, reconciliation, and payout approval process are implemented and tested. Do not claim automatic card charging or escrow today.
6. **Operations:** replace demo accounts/content, load real service areas and support contacts, verify nanny-review and dispute procedures, and publish Terms, Privacy, Safety, cancellation/refund rules.
7. **Security and observability:** use production error handling/logging, monitored backups, rate limits backed by shared infrastructure where needed, and a tested incident/restore process.
8. **Release:** sign the Android bundle with a protected production key, test it on physical small and large phones, and run the staging checklist in `/tests/manual`.

## Useful next automation

Prioritise API authorisation tests, booking/PIN status-transition tests, upload-security tests, and browser/device end-to-end tests. Their exact server URL and test credentials belong in secure CI variables, not this repository.
