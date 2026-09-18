# Production launch runbook

## Before deployment

1. Provision separate staging and production MySQL databases. Import `NannyApp.Shared/database/schema.sql`, then apply migrations in order through `migrate_v5_api.sql`.
2. Set every value in `.env.production.example` in the host's secret manager. Production must use HTTPS, non-root database credentials, SMTP delivery, and S3-compatible object storage.
3. Deploy `NannyApp.Web`, `NannyApp.Mobile/NannyApp/api`, and `NannyApp.Shared` so both PHP applications use the same production database and shared storage configuration. Keep `NannyApp.Shared` outside the public web root.
4. Set the Android release `API_BASE_URL` to the HTTPS API URL, create a signed release build, and test it on physical devices.
5. Remove or disable all `@nanny.app` seed/demo accounts. Replace placeholder support, privacy, legal, and emergency contact details with approved business details.

## Manual-payment operations

- A booking creates a `manual` payment with status `pending`; no card or gateway charge is made.
- After a nanny accepts, operations gives the parent approved payment instructions outside the app.
- Only an admin on the web portal may use **Record payment** after confirming receipt. This changes the payment to `paid` and `held`.
- After a checked-out booking, the parent can confirm completion. An admin resolves disputed cases from the web payments screen by releasing or refunding the held amount.
- Reconcile every recorded payment against the bank/payment reference daily. Do not mark a payment received without independent proof.

## Staging acceptance checklist

- Parent registration, booking, cancellation, messaging, support, and review work.
- Nanny profile verification, availability, accept/reject, PIN check-in, check-out, and earnings work.
- Admin functionality works in a browser and admin credentials are rejected by the native API.
- Booking conflicts, incorrect PIN lockout, cancellation, dispute, manual payment recording, release, and refund are tested.
- Password reset and verification email delivery are tested against the production SMTP provider.
- Uploads are private, retrievable through the application, backed up, and not served from the web root.

## Pilot and go-live

Start with a small, verified group of parents and nannies. Review support tickets, payout records, failed sign-ins, and booking disputes daily. Promote to a wider launch only after the pilot has completed real bookings without unresolved payment or safeguarding issues.
