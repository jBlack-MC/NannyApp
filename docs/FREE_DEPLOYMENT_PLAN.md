# Zero-cost deployment and testing plan

Owner requirement, 2026-09-28: no paid services. This supersedes the earlier AWS selection. Do not provision Lightsail/S3, buy a domain, enroll in paid app stores, or enable metered overages. Trials/credits are not a permanent free deployment plan. No account or service was provisioned by this work.

## Selected approach

| Need | Zero-service-cost approach | Status / limitation |
| --- | --- | --- |
| Development and automated tests | Existing computer, PHP, MariaDB, Python, Node and Android tooling; existing synthetic mail/provider tests | Already working; existing equipment, power and internet are assumed |
| Public PHP/API/database hosting | Evaluate a free HelioHost account and its supplied subdomain | Candidate, not validated or deployed; account availability and resource limits apply |
| Uploaded images/documents | Existing local storage driver; private directory outside the served webroot on the chosen host | No separate storage bill; shares the host's disk quota; must verify privacy and backup access |
| Test email | Existing loopback SMTP sink and fake Resend transport | Free, no provider account or real delivery required |
| Real account email | Evaluate host-provided mail using the existing configured PHP mail transport | Must verify allowed sender, limits and actual delivery before enabling pilot registration/recovery |
| Android distribution | Direct signed APK through the existing download endpoint | No store enrollment required; release URL, signing and device testing still outstanding; APK uses host disk/transfer quota |
| iPhone/iPad and desktop | Installable web app over the host's HTTPS subdomain | No native iOS/store release; real-device installation checks required |
| Backups | Encrypted exports downloaded to existing owner-controlled storage | No new subscription; restore drill required; a copy only on the hosting account is not a backup |
| Monitoring | Local scheduled HTTPS/DB/queue checks and manual pilot review | Runs only while the checking machine is on; no always-on monitoring guarantee |
| CI | Run existing tests locally first | Hosted CI optional only within verified free quotas with paid spending disabled; do not assume unlimited private-repo minutes |

HelioHost advertises PHP, databases and 1000 MB of free storage on its [official site](https://heliohost.org/). This makes it a candidate for this PHP application, not proof of production compatibility. Review [free account availability](https://wiki.helionet.org/Johnny) and actual account terms at signup. Do not buy faster provisioning or an upgrade. If no free slot is available, continue local testing while waiting.

No permanent free production host has yet passed this application's acceptance checks. A shared free host must not be used for real identity documents merely because signup succeeds.

## Owner checklist

- [ ] Register only for the free hosting plan if a slot is available. Choose its free subdomain; no purchased domain needed for evaluation.
- [ ] Confirm that the provider permits the intended pilot/commercial activity, native API clients and APK distribution under the free plan.
- [ ] Confirm HTTPS on the supplied subdomain, a supported PHP runtime, PDO MySQL/MariaDB and the database engine/version. Existing migrations contain MariaDB-specific syntax; validate compatibility on a disposable database first.
- [ ] Confirm private directories outside the public root, required Apache deny rules, PHP sessions, upload-size limits and database/file export access.
- [ ] Confirm scheduled CLI jobs if using the Resend worker, outbound HTTPS/cURL, and provider email/sender limits. Do not expose the CLI worker as an unauthenticated web endpoint to bypass missing scheduling.
- [ ] Record account inactivity/renewal rules, disk/CPU/transfer quotas, backup availability and suspension behavior. Never enable automatic paid upgrades.
- [ ] Share the non-secret subdomain, engine/version and limits with the developer. Provision secrets securely on the host; do not paste passwords/keys into chat.
- [ ] Test using synthetic users/files first: web and native API login, mail verification/recovery, authorized uploads, unrelated-user denial, download headers, migrations and restore.
- [ ] Publish a signed Android build only after the real HTTPS API URL is configured. Keep the signing key backed up privately. Test the iPhone/iPad web app on a physical device.
- [ ] Resolve outstanding review blockers before inviting more pilot users. If the host cannot satisfy required privacy/email/API checks, keep the public launch pending rather than weakening authorization or buying a service.

## Where images live

Set `NANNYAPP_STORAGE_DRIVER=local`. Keep uploads in the shared private storage directory outside the host's public root and serve them only through the existing authorized media controllers. Keep ownership and relative file paths in MariaDB; file bytes stay on disk. No second database, S3, public photo-sharing site or public Drive link is needed.

Database, uploads, backups left on the host and APK releases compete for limited space. Set an upload/storage budget after measuring the deployed footprint, monitor it, remove temporary files and reject additional uploads safely before exhausting disk. A total storage-quota guard is still implementation work; the current upload-size limit alone is not sufficient. Fix orphan-file deletion and verify backup/restore before depending on this arrangement for real documents.

## Services not selected

- AWS Lightsail/S3: earlier plan archived; credits do not guarantee indefinite zero cost. No AWS login is needed for the current plan.
- Cloudflare R2: useful free allowances but metered paid usage exists beyond them; excluded under this requirement. [Official pricing](https://developers.cloudflare.com/r2/pricing/).
- alwaysdata free hosting: its documented free plan excludes for-profit use, so it is not selected for this marketplace pilot. [Plan restrictions](https://help.alwaysdata.com/en/admin-billing/billing/public-cloud-prices/).
- Resend: implemented integration remains optional. The [free tier](https://resend.com/blog/new-free-tier) has sending limits; real sending still requires an authorized sender setup. A free hosting subdomain does not automatically provide DNS authority for Resend verification. Do not buy a domain to enable it under this plan.
- Apple/Google store distribution: defer paid enrollment. Direct APK and Home Screen web installation remain the zero-store-fee routes. Native iOS development/distribution is not complete.
- Static-only hosting: cannot run the current PHP/PDO backend merely by uploading the repository. Do not promise a free frontend service replaces the application's server/database.

## Completion

- [x] Free-only constraint recorded and earlier paid deployment selection superseded.
- [x] Existing local synthetic tests and distribution implementation identified.
- [ ] Free hosting account obtained and compatibility validated.
- [ ] Reliable permitted live email, private uploads, backups and resource limits verified.
- [ ] Release defects resolved and public pilot deployed.

No application configuration was switched and no existing cloud resources were deleted. If you created paid resources yourself, review the provider console and preserve necessary data before deciding what to remove.
