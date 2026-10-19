# Database sync and login recovery

The Supabase PostgreSQL database is the shared source for the deployed website
and Android API. The local Docker MariaDB database is a separate development
copy; it is kept compatible by running the same versioned migrations and seed
process, not by sharing live records.

## GitHub Actions settings

Add these repository settings:

- `NANNYAPP_PRODUCTION_URL` variable: the deployed HTTPS website URL.
- `NANNYAPP_DB_URL` secret: the Supabase session-pooler URL on port `5432`.
- `NANNYAPP_DEMO_PASSWORD` secret: the password used by the demo accounts.

Run **Actions → Supabase database sync**. It will apply migrations, repair the
named accounts and profiles, refresh synthetic bot data, check API health, and
perform a real `parent@nanny.app` login through the deployed API.

## Vercel settings

Vercel must point to the same database URL, either through `NANNYAPP_DB_URL` or
the equivalent `NANNYAPP_DB_HOST`, `NANNYAPP_DB_PORT`, `NANNYAPP_DB_NAME`,
`NANNYAPP_DB_USER`, and `NANNYAPP_DB_PASS` variables. Set `NANNYAPP_DB_DRIVER`
to `pgsql` and `NANNYAPP_DB_SSLMODE` to `require`.

If Vercel and GitHub use different Supabase projects, the seed workflow can pass
while the login page still fails because the web API is reading another
database.

## Local development

Use the Docker commands in `docs/LOCAL_DEVELOPMENT.md` for the local MariaDB
copy. To seed Supabase intentionally from a secure terminal, set
`NANNYAPP_DB_URL` and `NANNYAPP_DEMO_PASSWORD`, install
`scripts/requirements-supabase.txt`, and run:

```powershell
python scripts/bootstrap_supabase.py
```

Do not commit connection URLs, passwords, service-role keys, or JWT secrets.
