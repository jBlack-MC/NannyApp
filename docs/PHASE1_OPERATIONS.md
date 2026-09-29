# Phase 1 operating rules

All application deployments require `NannyApp.Shared`. Both clients use the same booking service, ledger helper and local storage protections. Apply migrations through [the CLI runner](../NannyApp.Shared/database/MIGRATION_ORDER.md) before deploying this code. v8 adds durable operational queues.

## Booking creation and rescheduling

Dates must exactly match `YYYY-MM-DD HH:MM[:SS]` or the equivalent `T` separator form and be in the future. Values are local to the configured server/business timezone; configure PHP and MariaDB consistently before deployment. Durations are 1?24 hours with at most one decimal place, matching the stored precision. Overnight sessions are not supported. Address and notes are bounded; every selected child must belong to the active, verified parent. Web booking now selects saved child records instead of accepting unverified free-text identities.

A nanny must be active, email-verified and profile-verified with a positive server-side hourly rate. Every requested minute must fit a published `nanny_availability` day; missing days mean unavailable. Availability edits affect new bookings and rescheduling, not existing commitments. Both editors validate their times and take the nanny lock.

Creation/rescheduling lock the stable nanny user row before checking booking rows. Pending, confirmed, in-progress and disputed bookings block overlap; adjacent end/start times are allowed. Rescheduling returns the booking to pending and invalidates the old PIN so the nanny must accept again. Its existing agreed ledger amount remains unchanged.

## Allowed actions

| Actor/action | Source state | Result / ledger rule |
| --- | --- | --- |
| Nanny accepts | pending | confirmed; new six-digit PIN, no payment invented |
| Nanny rejects | pending | rejected; pending payment fails, held paid entry marked refunded |
| Parent/nanny cancels | pending, confirmed | cancelled; same reversal rule; released entries rejected |
| Nanny checks in | confirmed | in_progress; exact PIN required and consumed |
| Parent regenerates PIN | confirmed | replaces PIN and resets failed attempts |
| Nanny checks out | in_progress, not yet checked out | records checkout once |
| Parent confirms | in_progress with checkout | completed; held paid entry released |
| Automatic completion | in_progress, checkout older than grace window, no parent confirmation | same atomic release; durable notification |
| Parent disputes | confirmed, in_progress, completed | disputed; existing ledger retained; admins notified |
| Admin records receipt | pending, confirmed, in_progress, completed | only pending manual payment becomes paid; completed jobs release it, other jobs hold it |
| Admin releases | in_progress, disputed, completed | completed; release held paid entry |
| Admin refunds | pending, confirmed, in_progress, disputed | cancelled; released entries rejected |
| Parent reschedules | pending, confirmed | pending; eligibility and overlap rechecked |

Every manual transition verifies the actor and current state while holding the booking lock, then commits booking changes, ledger changes and notification events together. PIN failures increment under lock; after five failures only parent regeneration unlocks it. Administrators cannot arbitrarily move bookings back into active states using the old override endpoint.

**Post-release disputes require manual review.** The released ledger is preserved, not put back on hold or silently reversed. A real bank transfer cannot be undone by changing a database field. Admins reconcile externally and retain an audit record; the automatic refund endpoint refuses released funds. All payment actions remain manual ledger bookkeeping, not payment-provider transactions.

## Durable workers

Schedule these CLI commands on the same host, using the same private database configuration:

```sh
php NannyApp.Shared/bin/deliver-notifications.php
php NannyApp.Shared/bin/delete-storage.php
```

Run each every minute (or the shortest supported free-host interval). Each invocation processes at most 100 jobs. Multiple workers use row locks with SKIP LOCKED. In-app notification insertion and acknowledgement share a transaction, so a failed delivery remains pending and a retry cannot duplicate a committed notification. A failing event stops that worker invocation; investigate recurring failures and rerun after correction. Monitor the oldest pending row and worker exit status.

Booking notifications are now durable **in-app** messages. The old synchronous booking email sends are not part of this flow; parents retrieve PINs in their authenticated booking view. Account verification/recovery mail continues through the existing mail/optional Resend integration. No external booking-mail delivery or provider exactly-once guarantee is claimed.

Document deletion removes the authorized reference only after its file key is saved in `storage_deletions` in the same transaction. Files are inaccessible through that removed reference immediately; the worker deletes bytes and acknowledges success. Missing files count as success; failures retry after five minutes, without a fixed retry limit. Pending failures require monitoring. Existing historical orphan objects need a reviewed inventory; they are not blindly purged by migration.

If the free host cannot schedule private CLI workers, do not expose a public worker URL or claim this deployment is ready. Pending events remain in the database until a worker runs.

## Local upload quota

Use `NANNYAPP_STORAGE_DRIVER=local` and optionally set `NANNYAPP_STORAGE_DIR` to an absolute private persistent directory. Defaults are a **100 MiB total upload-directory quota** (`NANNYAPP_STORAGE_QUOTA_BYTES=104857600`) and **20 MiB free-disk reserve** (`NANNYAPP_STORAGE_RESERVE_BYTES=20971520`). Invalid settings fail closed. Set lower values if the host needs more space for database, APKs, temporary uploads and backups.

All local uploads share a filesystem lock covering quota calculation and the final move. The guard counts existing files, rejects traversal/symlinks and refuses non-HTTP source files and existing destinations. Existing per-image MIME/size checks remain. Quota rejection does not create a document reference. The free-space check sees filesystem capacity; it cannot discover a provider's separate account quota or reserve space against unrelated processes. Configure a conservative upload budget and monitor the whole hosting account. S3 remains outside the selected free deployment and does not use this local filesystem quota.

## Validation and remaining operations

The Docker suite runs fresh/populated migrations, conflicting-data preservation, booking validation and concurrency, authorization, PIN limits, transaction failures, notification retries, document deletion and real HTTP image/quota checks using only synthetic data. Live-host migrations, off-host backup restoration, scheduled-worker behavior, browser/mobile acceptance and remaining unrelated review findings are separate launch gates.
