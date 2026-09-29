# Automatic checks and local Docker tests

Validation on 2026-09-28: full Docker suite passed with PHP 8.3 and MariaDB 10.11; Windows Apache denial check also passed. Android debug/unit/lint passed (0 lint errors, 52 warnings); missing release URL was rejected and an explicitly configured unsigned release compiled. Merged manifest retains other Startup initializers and removes WorkManager default metadata. No GitHub-hosted run or device execution is claimed.

This project uses free local tools and quota-limited GitHub Actions. No workflow deploys to a host, creates cloud resources, sends real email or publishes a signed application.

## What runs automatically

On pushes, pull requests, manual dispatch and Mondays at 04:23 UTC (06:23 South Africa time), CI runs two jobs:

1. **PHP and synthetic security tests:** builds the test image and starts a disposable MariaDB; runs PHP/JavaScript syntax checks, real HTTP/auth/media/recovery/rate-limit tests, fake-provider Resend failure tests, Apache access-denial tests, offline DAO/PWA regressions and app install/download tests.
2. **Android build, unit tests and lint:** debug assembly, JVM tests, lint, a negative check that a missing release URL fails, and an unsigned release build using a synthetic non-routable API hostname. The release is compilation evidence only, not a publishable app.

Old runs are cancelled when a newer revision arrives. Jobs have timeouts, read-only repository permissions, no provider/signing secrets and no artifact uploads. A weekly schedule reruns the suite even without code changes. Scheduled jobs consume runner time and retain the same private-repository free-billing gate. Checks execute when triggered; they are not always-running production monitoring.

For a **private repository**, both jobs remain disabled until the owner sets repository Actions variable `FREE_CI_CONFIRMED=true` after verifying that paid Actions/cache/storage overages are disabled in billing. This repository cannot change or guarantee your account billing settings. Standard public-repository runners are free; private repositories have included quotas and possible overage charges. See [GitHub billing](https://docs.github.com/en/billing/concepts/product-billing/github-actions). Do not expose customer data or make a repository public merely to get runner minutes.

## GitHub failure diagnosed on 2026-09-28

[Run 36486157818](https://github.com/jBlack-MC/NannyApp/actions/runs/36486157818) failed before any steps ran. Both job annotations say: **?The job was not started because your account is locked due to a billing issue.?** This is an account-level block, not a failing application test or missing repository variable. The repository was public when inspected. Workflow edits cannot unlock the account.

Owner actions:

1. Open GitHub personal **Settings ? Billing and licensing** (or the owning organization's billing settings). Inspect the lock notice and contact GitHub Support if the account is incorrectly restricted. Resolving an existing account obligation is an owner decision; this project does not require purchasing a plan.
2. Keep paid Actions overages disabled. Do not add a paid runner or enable spending to bypass the problem. Use local Docker checks while the account is blocked.
3. Commit/push the workflow changes, then open **Actions ? CI ? Run workflow** after the account restriction is cleared. A rerun of an old failed run uses its original revision; use the updated branch for these changes.
4. Confirm both CI jobs actually start and pass. Enable Actions failure notifications in your GitHub notification settings. No remote success is claimed until a new run passes.

## Inspect the latest remote failure locally

Run `python tests/check-actions.py` from this checkout with GitHub CLI installed and authenticated. It reads the repository's latest run, lists failed steps and prints job annotations even when runner startup failed and no log exists. It does not push, rerun jobs, change settings or access billing credentials. Exit codes: 0 for a successful latest run; 1 for a completed non-successful run; 2 for pending/no run or diagnostic errors. The latest run may be from another branch or workflow; check the printed name, URL and revision.

Verified on 2026-09-29 against the actual failed run: both job annotations report the account billing lock, with no executed steps. Future CI runs also write concise security and Android outcomes to the GitHub job summary. These summaries require a runner to start; the local diagnostic command handles startup failures.

## Workflow validation and recurring checks

`workflow-checks.yml` runs actionlint 1.7.12 on workflow changes, manual dispatch and Mondays at 04:41 UTC. It checks YAML, expressions and embedded shell commands. The CI jobs use `ubuntu-24.04` to avoid the announced automatic migration of `ubuntu-latest` to Ubuntu 26. Both workflows have read-only permissions, cancellation of superseded runs and timeouts; no artifact storage, deployment or production credentials are used.

Schedules run from the default branch and may be delayed or disabled by GitHub for inactive public repositories. They are weekly regression checks, not production uptime monitoring. The account lock blocks scheduled runs too. Keep workflow validation optional as a branch requirement because its path filters skip unrelated changes.

Local workflow validation from PowerShell at the repository root:

```powershell
docker run --rm --mount "type=bind,source=$((Get-Location).Path)/.github/workflows,target=/workflows,readonly" rhysd/actionlint:1.7.12 /workflows/ci.yml /workflows/workflow-checks.yml
```

The image and command follow [actionlint's Docker usage](https://github.com/rhysd/actionlint/blob/main/docs/usage.md#docker).

## Run the same checks locally

From the repository root with Docker Engine/Compose running:

```sh
python tests/docker-checks.py
```

The wrapper builds from an explicit source-only archive (avoiding Windows build-context transfer failures), runs Compose, and performs cleanup even when checks fail. Cleanup applies only to the `nannyapp-tests` Compose project. The database lives in container tmpfs, has no host-published port, and the tests share its network namespace to use their required loopback port 13379. No application database, host credentials, real uploads, mail logs or signing files are mounted. The image is test-only, not a production deployment image. First use downloads public images/packages and requires internet; no hosted paid service is needed.

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
