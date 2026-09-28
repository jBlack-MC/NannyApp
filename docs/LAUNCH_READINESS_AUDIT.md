# Production readiness audit

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
