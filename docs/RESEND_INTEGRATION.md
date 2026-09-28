# Queued account email through Resend

Budget update: Resend activation is optional under the [free-only plan](FREE_DEPLOYMENT_PLAN.md). Use existing synthetic email tests now; do not purchase a domain or paid mail plan to activate this integration. Live free sender/delivery compatibility remains unverified.

## Completion status - 2026-09-28

- [x] Account-email adapter, transactional outbox, worker and synthetic failure regressions implemented and passed.
- [ ] Apply v7 to the pilot database, configure sender/key and schedule the worker.
- [ ] Verify live delivery, queue alerts and operational recovery.

Implementation is complete; provider activation and production verification are outstanding.

Implemented for the small-team pilot: verification and password recovery. Online
payments remain disabled. No real provider request or real email was sent during
implementation.

## Enable it

1. Apply `NannyApp.Shared/database/migrate_v7_email_outbox.sql` after v6 using the
   deployment migration account. Do not import the destructive development schema.
2. Verify a sender domain with Resend. Put a send-only key into the **worker's**
   server environment as `NANNYAPP_RESEND_API_KEY`. Never put it in Android,
   JavaScript, Git, a public config file or command-line arguments.
3. Set web/API `NANNYAPP_MAIL_TRANSPORT=resend`, `NANNYAPP_MAIL_FROM` to the verified
   sender, and `NANNYAPP_BASE_URL` to the absolute HTTPS web portal URL. The web/API
   processes enqueue messages and do not need the provider key.
4. Run `php /srv/nannyapp/current/NannyApp.Shared/bin/send-email.php 5` every minute
   under a dedicated service account/systemd timer with database credentials and
   the provider key supplied through a protected environment file. Enable PHP's
   cURL extension and a current CA bundle. Five jobs bound a normal run to roughly
   50 seconds of provider waits, plus local database work.
5. Alert on worker failure, failed jobs, and pending jobs older than five minutes.
   Review provider bounce/delivery information in its dashboard during the pilot.
   Provider acceptance is not proof that a message reached an inbox.

The existing explicitly configured `mail` transport remains available. There is
no automatic cross-provider fallback after an ambiguous result: that could send
duplicates. Stop the worker before changing transports and resolve pending jobs.

## Code and failure behavior

| Code | Responsibility |
| --- | --- |
| `Shared/config/email.php` | Routes verification/recovery to the selected transport. |
| `Shared/config/email_outbox.php::queue_account_email` | Validates the recipient against the locked server user; commits token and immutable payload together. |
| `Shared/config/email_outbox.php::claim_account_email` | Claims a job with a 120-second lease and a random ownership token; independent workers cannot claim the same live lease. |
| `Shared/config/resend.php::resend_http` | Fixed HTTPS origin, TLS verification, no redirects, 3-second connect and 10-second total timeout, 64-KiB response cap, cURL cleanup in `finally`. |
| `Shared/config/resend.php::resend_deliver` | Validates response status and UUID-shaped message ID; returns sanitized result codes. |
| `Shared/config/email_outbox.php::deliver_account_email` | Checks lease and token validity; retries or finalizes under the same lease fence; removes terminal payloads. |
| `Shared/bin/send-email.php` | CLI-only bounded worker; emits job IDs/statuses, never recipients, links, bodies or secrets. |

Paths beginning `Shared/` above mean `NannyApp.Shared/`.

- **Slow/down provider:** signup commits only the database queue entry and returns;
  it does not wait on the provider. The worker times out, persists exponential
  backoff with jitter, and tries at most eight times. Account actions still work;
  unverified users remain unable to log in until mail recovers and they verify.
- **429, 408, 409 or 5xx:** retry with the same payload/key; numeric `Retry-After`
  is honored. Other non-success statuses, including unauthorized credentials and
  validation errors, terminate that job for operator attention. Resend documents
  rate-limit delays in its [usage-limit headers](https://resend.com/docs/api-reference/rate-limit).
- **Malformed/oversized success body:** do not mark sent. Treat it as uncertain
  and retry under the existing key. No untrusted error text is rendered/logged.
  Success requires the documented message ID shape from the
  [send-email response](https://resend.com/docs/api-reference/emails/send-email).
- **Provider accepted; local acknowledgement failed/crashed:** the lease expires,
  another worker reclaims the row, and sends exactly the same persisted payload
  and `nanny-email/<job-id>` key. Resend retains idempotency keys for 24 hours;
  jobs expire within 23 hours of creation (reset jobs within 3,500 seconds), so
  automatic retries stay inside that window. See its
  [idempotency contract](https://resend.com/docs/dashboard/emails/idempotency-keys).
  This is bounded deduplication, not an unconditional exactly-once guarantee.
- **Queue save fails:** token changes roll back and no provider request occurs.
  Registration can remain created but unverified; users can request another link.
  Public recovery responses remain neutral to avoid account enumeration.
- **User resends/verifies/resets before a queued message is dispatched:** stale
  verification links and used/expired reset links are skipped before sending.
  A change racing with an already-in-flight delivery can still produce an obsolete
  email; its token fails at consumption and the user can request a fresh link.
- **Permanent failure, retry exhaustion, or expired link:** stop automatically.
  `failed` means delivery was not confirmed, not proof of nondelivery. Do not
  blindly create a replacement job after an ambiguous acceptance. Review the
  provider/job ID and let the user request a fresh link if necessary.

Queued payloads contain private email/link data in the database. Restrict DB
access and encrypt backups. Terminal jobs clear the payload; remove terminal
metadata older than seven days on a daily schedule. Do not log payloads or query
strings containing account tokens.

## Verification

On a disposable local MariaDB server, port 13379 (not the app database):

```text
php tests/security/resend.test.php
python tests/security/run-backend.py /path/to/php
```

The Resend suite injects a fake HTTP boundary; it tests timeout/exception handling,
retry classification, response shape/size, leases, exact key/payload reuse after
simulated partial success, stale workers, backoff, expiry, supersession and atomic
enqueue rollback. The existing SMTP/HTTP suite verifies that the alternative mail
transport and account endpoints still work. Real TLS/provider/domain deliverability
must be checked with a designated team-owned test inbox before enabling the pilot.
