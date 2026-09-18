# Production readiness audit

## Implemented project boundaries

- The Android app is for parents and nannies; administrator navigation is not exposed there.
- Web administration remains available through the PHP web portal.
- The native app uses the shared logo-only `assets/Icon_Logo.png` artwork and a mobile-safe Compose design system aligned to the web palette.
- The project now has a repeatable test home under `/tests` and an Android unit-test baseline.

## Required before a public online launch

These items need real provider choices and production credentials, so they cannot safely be completed with source-code placeholders.

1. **Hosting and domain:** configure production PHP/MySQL hosting, HTTPS certificates, DNS, firewall rules, backups, and a real Android `API_BASE_URL`. The release placeholder `https://your-domain.example.com/nannyapp/api/` must never ship.
2. **Secrets:** create environment-specific database, SMTP, session, and API credentials outside source control. Rotate any credential that was ever committed or shared.
3. **Email:** configure and verify an SMTP provider for account verification, password reset, booking, and support emails; test delivery and sender-domain alignment.
4. **Uploads:** move identity documents and profile uploads off the public web root (or use private object storage), enforce MIME/size checks, and verify backup/restore.
5. **Payments:** keep launch copy and backend status manual/offline until a payment gateway, webhook signature verification, idempotency, refund policy, reconciliation, and payout approval process are implemented and tested. Do not claim automatic card charging or escrow today.
6. **Operations:** replace demo accounts/content, load real service areas and support contacts, verify nanny-review and dispute procedures, and publish Terms, Privacy, Safety, cancellation/refund rules.
7. **Security and observability:** use production error handling/logging, monitored backups, rate limits backed by shared infrastructure where needed, and a tested incident/restore process.
8. **Release:** sign the Android bundle with a protected production key, test it on physical small and large phones, and run the staging checklist in `/tests/manual`.

## Useful next automation

Prioritise API authorisation tests, booking/PIN status-transition tests, upload-security tests, and browser/device end-to-end tests. Their exact server URL and test credentials belong in secure CI variables, not this repository.
