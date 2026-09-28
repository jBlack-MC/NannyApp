# Deployment plan: small team, pilot users

## Current decision: free services only

The owner has replaced the AWS/paid-host selection with a strict zero-cost requirement. [FREE_DEPLOYMENT_PLAN.md](FREE_DEPLOYMENT_PLAN.md) is now authoritative for hosting, storage, accounts, testing and distribution. The VPS/AWS choices below are retained as a historical alternative and must not be provisioned. General security, migration and rollback requirements still apply; free shared hosting may not support the immutable-release/Apache reload mechanism below.

## Completion status - 2026-09-28

- [x] Deployment, configuration, monitoring and rollback plan written for the confirmed pilot scope.
- [x] Legacy destructive deployment instruction corrected.
- [x] Security fixes and optional Resend integration implemented with synthetic regression evidence.
- [ ] Resolve remaining release/security/booking defects in the [code review](CODE_REVIEW_2026-09-27.md).
- [ ] Provision the actual host, secrets, migrations, mail worker, monitoring and backups.
- [ ] Complete restore, live-mail, browser/device and signed-release acceptance checks.

No deployment has been performed. The remaining code fixes are detailed in the [review](CODE_REVIEW_2026-09-27.md); rollout requirements follow below.

Targets confirmed by the project owner: desktop/mobile browsers and native Android.
User/traffic counts, current hosting, recovery targets and data-residency obligations
are not provided. This plan assumes a small invitation-only pilot that fits on one
host, not a public-scale launch. Resolve the high-severity items in
`CODE_REVIEW_2026-09-27.md` before expanding the pilot.

## AWS selection - 2026-09-28

AWS is now selected. Use one Lightsail Linux host for PHP/MariaDB and private S3 for uploads; the local-storage option below remains a development/rollback alternative. Follow [AWS_OWNER_CHECKLIST.md](AWS_OWNER_CHECKLIST.md) for owner account actions, storage configuration and app publishing. No resources have been provisioned. iPhone/iPad and other devices are served through the installable web app; a native iOS client remains separate work.

## Where it runs

Use one managed Linux VPS in one region near the pilot users, with Apache 2.4,
PHP-FPM, and a supported MariaDB package bound to loopback. Use PHP 8.3 initially
to match repository CI, with an upgrade planned within its support window; the
local XAMPP PHP 8.1 runtime is not a production baseline. Check the official
[PHP support table](https://www.php.net/supported-versions.php) when provisioning.
Have one team member own patching, backups and incident response. If nobody can
operate this host, buy managed PHP/database hosting that supports CLI scheduled
jobs, private storage and immutable releases instead of leaving it unmanaged.

Keep uploads on a persistent private directory outside the served webroot with
encrypted off-host backups. S3 is optional at this stage, not a launch prerequisite;
keep an existing S3 deployment private if already in use. No public document CDN.
Use queued Resend for account emails; start with manual payment reconciliation.

Preserve the monorepo paths within each immutable release. Serve only
`NannyApp.Web`, and alias `/api/` to that release's
`NannyApp.Mobile/NannyApp/api/`. Include the mobile `config` and `includes` folders
in the release but never expose them as URLs. Keep `NannyApp.Shared` outside the
public document root. A symlink from each release's `NannyApp.Shared/storage`
to the persistent uploads directory accommodates the current storage constant.
Use one explicit host/base path; the existing Apache rules assume `/nannyapp`, so
adapt and test them for the chosen mount. Block migration/seed scripts from HTTP.

This single host is an accepted pilot failure point. It does not provide high
availability or survive a host failure without restoration.

## Configuration and secrets

`.env.production.example` is a checklist, not an automatically loaded dotenv file.
Provision independent pilot DB passwords, sender identity and Resend key in
root-owned environment files outside releases/docroot, readable only by the
required service accounts. Give the web DB user data privileges and a separate
migration account schema privileges. Give the worker a send-only Resend key;
the web/API processes only need queue access. Never ship any key in an APK.

Explicitly allow required environment values into PHP-FPM: it clears worker
environment variables by default. Use pool `env[...]` configuration, not a public
`phpinfo()` page, to verify settings. See the
[PHP-FPM configuration reference](https://www.php.net/manual/en/install.fpm.configuration.php).
Keep environment files across release changes. Rotate secrets individually and
restart/reload only affected services; don't copy local root/demo credentials.

Set HTTPS at Apache and avoid trusting client-supplied forwarded headers at an
unprotected origin. Deny public DB ports. Remove demo accounts, synthetic fixtures,
test routers, `.codex`, `.git`, email logs and development credentials from the
release artifact. Signing keys stay in the Android release operator's private
keystore/secret storage. Set a real release API URL before building the signed APK.

## Releases without planned code-deploy downtime

1. Build one immutable artifact from a reviewed revision. Run PHP lint, the
   disposable security/mail tests, Android unit tests/debug assembly and the
   known-failing lint gate after fixing its WorkManager error. Smoke the artifact
   locally with synthetic data; don't copy customer data into developer databases.
2. Back up the live database and upload inventory; confirm a recent restore test
   to a separate disposable database. **Never import the development `schema.sql` into an existing database:
   it drops `nanny_app`.** Use reviewed additive
   migrations, including v6 and v7 when enabling the new features.
3. Unpack into `/srv/nannyapp/releases/<revision>` and wire persistent storage,
   sessions and environment. Validate configuration, schema compatibility and
   route permissions on a loopback-only candidate virtual host. Do not test by
   creating fake bookings in the live customer database.
4. Apply only migrations compatible with both old/new code before switching.
   Check locks and runtime on a disposable copy first. If a schema operation
   cannot run safely online, schedule a short write pause; this codebase cannot
   honestly promise zero downtime for arbitrary database migrations.
5. Point both web and API Apache paths to the new **absolute release path**, run
   `apachectl configtest`, then gracefully reload Apache. Old requests retain old
   release files; new requests use the new release. Keep both directories while
   requests drain. Apache's [graceful restart](https://httpd.apache.org/docs/2.4/stopping.html)
   lets active requests complete. Keep PHP sessions outside release directories.
6. Pause the email timer during its release-path switch, let the bounded old
   worker finish, update `/srv/nannyapp/current`, then resume it. Pending jobs
   survive in the database. Check the public login page, an authorized designated
   test account and error/queue metrics immediately after switching.
7. Ship the signed Android release after the backward-compatible API is live.
   Keep older app versions' contracts supported; users do not upgrade together.
   CSS/JS should use revisioned filenames before frequent mixed-version releases.

No automated deployment is installed by this plan. The legacy launch runbook was
corrected on 2026-09-28 to reference this plan; the new mail migration is provided
but not applied to any application/pilot database.

## Know when it breaks

Use one external HTTPS check every few minutes and alert two team members on
failure. Add a tiny readiness route that checks a DB round-trip and returns only
ready/unready; until implemented, use an internal scheduled DB check in addition
to the public check. Alert on repeated 5xx, worker nonzero exits, oldest due mail
over five minutes, failed mail jobs, low disk space and backup failures. Rotate
server logs; exclude Authorization/Cookie headers, request bodies and account-link
query strings. Review a small daily pilot report: booking failures, unresolved
disputes, manual payment discrepancies and email queue health.

Take encrypted off-host DB/upload backups and rehearse a restore before the next
pilot release. Agree a recovery-point objective with the team: nightly backups
alone can lose almost a day of bookings. If that is unacceptable, enable binlog
archival and more frequent snapshots before relying on the system for real work.

## Rollback

On increased errors or broken booking/login smoke checks, stop the worker timer,
restore Apache paths and the worker symlink to the prior release, config-test and
gracefully reload. Leave additive tables/columns in place. Do not automatically
run down-migrations or restore yesterday's database: either can destroy new pilot
bookings. For data corruption, pause writes, take a new forensic backup, restore
into a separate DB, reconcile post-backup transactions, then deliberately switch.
An email already accepted by a provider cannot be unsent; preserve outbox/job IDs
across rollback and don't replay completed work with new keys.

## Deliberately omitted

| Omission now | Specific trigger for adding it |
| --- | --- |
| Permanent separate staging stack | Weekly concurrent feature releases, payment-provider integration, or schema changes too risky to reproduce in disposable tests; use isolated synthetic data. |
| Container orchestration/Kubernetes | Multiple independently scaled services or enough app instances that manual process management becomes the measurable bottleneck. Containers alone do not require orchestration. |
| Multi-region hosting | A contractual availability/residency need or measured distant-user latency; first solve same-region recovery and database replication. |
| Elaborate CI/CD/canary platform | Manual releases become frequent/error-prone or multiple people deploy concurrently. Add a small serialized deploy script and automated rollback checks first. |
| Redis, queue broker, search cluster | Measured DB contention, sustained queue backlog or search latency; the database outbox and existing SQL are sufficient for a small pilot. |
| Dedicated managed DB/high availability | Restore time or single-host outages exceed the pilot's agreed tolerances, or memory/IO measurements show DB/app contention. |
