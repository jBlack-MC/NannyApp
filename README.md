# Nanny-App

Childcare booking for parents and nannies, with browser-based administration. The PHP website and Android API share accounts, bookings, messages, uploads and a manual payment ledger through `NannyApp.Shared`.

[Website](https://nannyapp-two.vercel.app) · [Download Android](https://nannyapp-two.vercel.app/download-app.php?platform=android) · [Documentation](docs/README.md) · [CI runs](https://github.com/jBlack-MC/NannyApp/actions)

## Sign in and demo accounts

**`Password123!` is a local development fixture password. It does not work for the hosted demo accounts.** Use the private password issued for the specific account. On the maintainer's existing checkout, these are in the ignored `.codex/demo-login-credentials.md` file; that file is not distributed with the repository. Ask the deployment owner for access if you do not have it.

| Role | Email | Client |
| --- | --- | --- |
| Admin | `admin@nanny.app` | Normal web browser only |
| Parent | `parent@nanny.app` | Web and native Android |
| Parent | `james@nanny.app` | Web and native Android |
| Verified nanny | `amelia@nanny.app` | Web and native Android |
| Pending nanny | `grace@nanny.app` | Web and native Android; verification restrictions apply |

Supabase keys and the database password are not application login passwords. Both clients authenticate against the same application `users` records; Supabase Auth is not the account system. New accounts follow the application's verification rules. See [demo account operations](docs/DEMO_ACCOUNTS.md) for explicit seeding and password rotation. Schema upgrades preserve existing passwords. Disable demo accounts before admitting real users.

Install Android from the website download link, rather than an old `app/release/` file. Release APKs must point to `https://nannyapp-two.vercel.app/api/`. A successful web login does not prove that an older APK uses the correct server.

## Architecture

| Component | Implementation | Guide |
| --- | --- | --- |
| Website and administration | PHP 8.3, PDO, vanilla JavaScript, PWA | [Web](NannyApp.Web/README.md) |
| Android | Kotlin, Jetpack Compose, Hilt, Retrofit, Room | [Mobile](NannyApp.Mobile/README.md) |
| JSON API | PHP under `NannyApp.Mobile/NannyApp/api/`, served at `/api/` | [API and Android setup](NannyApp.Mobile/NannyApp/README.md) |
| Shared services | Authentication, migrations, storage, queues and booking rules | [Shared](NannyApp.Shared/README.md) |
| Database | Supabase PostgreSQL for the hosted app; MariaDB supported locally | [Migration order](NannyApp.Shared/database/MIGRATION_ORDER.md) |

Android communicates with the API over HTTPS; it never connects directly to PostgreSQL. Keep `NannyApp.Shared` alongside both PHP frontends. Private media is served through authorized endpoints. Storage supports local disk, database-backed prototype uploads and private S3-compatible storage.

Parents can manage children, request bookings, communicate with nannies and review completed sessions. Nannies manage profiles, availability, bookings and check-in/out. Admins handle verification, support, disputes and the operations dashboard. Payments are manual: held/released/refunded values describe ledger decisions, not automatic bank transfers or escrow services.

## Run locally

Install Docker Desktop, then run from the repository root:

```sh
docker compose -f deploy/local/compose.yml up -d --build
docker compose -f deploy/local/compose.yml run --rm seed
```

Open `http://localhost/`. The local seed is for development only; rerunning it can add duplicate sample bookings. See [local development](docs/LOCAL_DEVELOPMENT.md) for ports, emulator/phone addresses and stopping the stack.

For Android, open `NannyApp.Mobile/NannyApp` in Android Studio. Use JDK 21 and the SDK configuration in Gradle. The emulator defaults to `http://10.0.2.2/nannyapp/api/`; a physical phone needs a reachable LAN or HTTPS API address.

## Test before merging

```sh
python tests/docker-checks.py
python tests/docker-checks.py --postgres
python tests/check-readmes.py
```

From `NannyApp.Mobile/NannyApp`:

```sh
./gradlew --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Use `gradlew.bat` on Windows. Database suites create disposable synthetic databases and clean them up. They cover registration/login, authorization, shared web/API data, migrations, session revocation and cache boundaries. [Test guide](tests/README.md).

## CI/CD

| Pipeline | Trigger | Outcome |
| --- | --- | --- |
| CI | Pull requests, main pushes, manual and weekly | MariaDB/PHP security, PostgreSQL integration, Android build/unit/lint, README and deliverable checks |
| Workflow validation | Pull requests, main pushes, manual and weekly | actionlint checks workflow syntax and shell expressions |
| Vercel integration | Connected repository PRs and production branch | Preview/production web and API deployment; configured in Vercel |
| Production verification | Successful production deployment, manual and daily | Read-only health, login/register availability, private API denial and cache headers |
| Supabase database sync | Manual on protected `main` | Versioned schema upgrade; optional explicit demo seeding/reset |
| Android release | Manual on protected `main`, existing merged version tag | Signed APK, backend URL/signature checks and GitHub release |
| Demo accounts | Manual on protected `main` | Create missing named demos using a private secret |

[CI/CD setup and release sequence](docs/CI_CD.md) lists variables, secrets, deployment gates and rollback. Repository review/check requirements still apply. Production credentials are not used in pull-request tests. Private repositories use the existing `FREE_CI_CONFIRMED` gate; configuring workflows does not guarantee runner availability or hosting configuration.

## Database and operations

Use `php NannyApp.Shared/bin/migrate.php` with explicit database configuration, or the Supabase sync workflow. Shared migrations currently extend through v14 (contact inbox), followed by PostgreSQL-specific access restrictions. Runners track checksums and reject changes to applied migrations. Never replace a populated database with a fixture or historical bootstrap dump.

The operations page at `/admin/operations.php` requires the contact inbox and queue tables. Missing tables indicate an incomplete schema upgrade. Trace error reference codes in server logs, apply the versioned migrations, and check `/api/health.php`. Do not hide schema failures with empty dashboard counts.

Authentication and private account responses must not be cached. The PWA caches public content; Android's private caches are scoped to the account and cleared at session boundaries. Other devices observe changes when they refresh/poll; this is not a promise of instantaneous offline synchronization. Logout revokes the account's sessions across clients on their next authenticated request.

## Deployment and project documents

- [Vercel + Supabase](docs/VERCEL_SUPABASE_DEPLOY.md), [AWS alternative](docs/AWS_DEPLOYMENT.md), [troubleshooting](docs/TROUBLESHOOTING.md)
- [Documentation index](docs/README.md), [HTML/PDF deliverables](docs/deliverables/README.md), [engineering audit](docs/deliverables/08-professional-engineering-audit.html)
- [Demo video](https://youtu.be/nO85AqjW2E4); module XISD6329, Work Integrated Learning 3B

Live device acceptance, external email delivery, backup restoration and monitoring still require deployment evidence. Never publish passwords, database URLs containing credentials, signing keys or private test logs.
