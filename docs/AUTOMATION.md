# Automatic checks and local Docker tests

This project uses free local tools and quota-limited GitHub Actions. No workflow deploys to a host, creates cloud resources, sends real email or publishes a signed application.

## What runs automatically

On pushes, pull requests and manual dispatch, CI runs two jobs:

1. **PHP and synthetic security tests:** builds the test image and starts a disposable MariaDB; runs PHP/JavaScript syntax checks, real HTTP/auth/media/recovery/rate-limit tests, fake-provider Resend failure tests, Apache access-denial tests, offline DAO/PWA regressions and app install/download tests.
2. **Android build, unit tests and lint:** debug assembly, JVM tests, lint, a negative check that a missing release URL fails, and an unsigned release build using a synthetic non-routable API hostname. The release is compilation evidence only, not a publishable app.

Old runs are cancelled when a newer revision arrives. Jobs have timeouts, read-only repository permissions, no provider/signing secrets and no artifact uploads. No recurring schedule consumes minutes while the project is idle. Checks execute when triggered; they are not always-running production monitoring.

For a **private repository**, both jobs remain disabled until the owner sets repository Actions variable `FREE_CI_CONFIRMED=true` after verifying that paid Actions/cache/storage overages are disabled in billing. This repository cannot change or guarantee your account billing settings. Standard public-repository runners are free; private repositories have included quotas and possible overage charges. See [GitHub billing](https://docs.github.com/en/billing/concepts/product-billing/github-actions). Do not expose customer data or make a repository public merely to get runner minutes.

## Run the same checks locally

From the repository root with Docker Engine/Compose running:

```sh
docker compose -f compose.test.yml up --build --abort-on-container-exit --exit-code-from tests
docker compose -f compose.test.yml down --volumes --remove-orphans
```

Cleanup applies only to the `nannyapp-tests` Compose project. The database lives in container tmpfs, has no host-published port, and the tests share its network namespace to use their required loopback port 13379. No application database, host credentials, real uploads, mail logs or signing files are mounted. The image is test-only, not a production deployment image. First use downloads public images/packages and requires internet; no hosted paid service is needed.

Without Docker:

```sh
python tests/run-checks.py --php /path/to/php
python tests/run-checks.py --php /path/to/php --backend
```

The second command requires the disposable loopback database described in [the security test README](../tests/security/README.md). Linux Apache checks run when `apache2` is installed; Windows Apache remains available via `python tests/security/apache.test.py C:/xampp/apache`.

Android commands from `NannyApp.Mobile/NannyApp`:

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
./gradlew :app:assembleRelease -PNANNYAPP_RELEASE_API_URL=https://YOUR-ACTUAL-HOST/api/
```

Use `gradlew.bat` on Windows. The release URL must be explicit HTTPS with a trailing slash, without credentials/query/fragment. Environment variable `NANNYAPP_RELEASE_API_URL` is also supported. Debug still uses the emulator host address. Production signing and physical-device testing remain owner/release tasks; CI does not have those keys.

## Why no Kubernetes

The planned free PHP host does not need a cluster. Kubernetes adds cluster operations, networking, persistence and monitoring without fixing the current booking/security defects or providing free hosting. Docker here makes tests reproducible. Revisit a local Kubernetes exercise only if learning Kubernetes becomes an explicit goal, or production orchestration when measured multi-service/multi-instance needs justify it. No Kubernetes manifests or paid cluster were created.

## Owner actions

- [ ] Confirm free-only Actions billing settings; enable `FREE_CI_CONFIRMED` for a private repository only afterward.
- [ ] Push these files to run the remote jobs; local execution does not prove GitHub execution.
- [ ] Where your repository plan supports it, require both CI jobs before merging. Branch-protection settings are external to this checkout and were not changed.
- [ ] Resolve remaining review findings; passing the existing regressions is not a complete security certification.
- [ ] Keep signed release/device checks, live free-host/mail/storage compatibility and backup/restore drills on the launch checklist.

Default-initializer removal follows [Android WorkManager configuration guidance](https://developer.android.com/develop/background-work/background-tasks/persistent/configuration/custom-configuration). Database readiness uses Compose's [healthcheck dependency](https://docs.docker.com/compose/how-tos/startup-order/).
