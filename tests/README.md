# Project tests

Run from the repository root unless noted otherwise. Use synthetic data and disposable databases; never point integration tests at production.

| Command | Coverage |
| --- | --- |
| `python tests/docker-checks.py` | PHP/JavaScript checks, MariaDB migrations, real HTTP auth/authorization, mail workers, booking operations, media, PWA and cache regressions |
| `python tests/docker-checks.py --postgres` | PostgreSQL schema/seed replay, native migration adoption, checksum drift, database privileges, parent/nanny registration and cross-client data |
| `python tests/check-readmes.py` | README relative links and accidental credential patterns |
| `python tests/deploy-database.test.py` | Production migration helper behavior without live secrets |
| `python tests/apk-endpoint.test.py` | APK endpoint validation using synthetic archives |
| `node tests/security/service-worker.test.cjs` | Public caching and private/auth route exclusion |

Docker wrappers build a source-only image and clean their test stacks. MariaDB uses loopback port 13379; PostgreSQL uses 13378. Do not run concurrent copies of the same stack.

From `NannyApp.Mobile/NannyApp`, run `./gradlew --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` (`gradlew.bat` on Windows). Release compilation requires an explicit API URL; CI uses a synthetic endpoint, never a production signing key.

[Security tests](security/README.md) · [API coverage](api/README.md) · [manual acceptance](manual/launch-smoke-checklist.md) · [CI/CD](../docs/CI_CD.md)

Deployment verification is read-only and separate from synthetic integration tests. It checks availability, health, cache policy and anonymous access denial; it does not prove real-device login or external email delivery. Record those results separately with the actual device, app version and backend URL.
