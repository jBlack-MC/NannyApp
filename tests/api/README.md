# API integration coverage

API regressions are implemented in the shared test suites, not just a staging backlog:

- `python tests/docker-checks.py`: synthetic HTTP authentication, token/session revocation, ownership checks, booking transitions, PIN limits, uploads, mail and rate limiting on MariaDB.
- `python tests/docker-checks.py --postgres`: real PostgreSQL migrations, private demo seeding, web/API registration for parents and nannies, cross-client login, profile/message/child data sharing and logout.

Run from the repository root. These commands create disposable local data and do not use production credentials. Admin API login must remain rejected; admin access belongs to the web browser.

[Security details](../security/README.md) and [test commands](../README.md). Hosted API health and anonymous access are checked by the production verification workflow. Physical Android networking, external email and uploaded-file readback still require the [manual checklist](../manual/launch-smoke-checklist.md).
