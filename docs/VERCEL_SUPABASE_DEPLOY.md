# Vercel + Supabase deployment

This is the free-tier deployment path for the PHP web app and Android API.
The existing AWS/MySQL deployment can remain online until this one is tested.

## Vercel project

When importing `jBlack-MC/NannyApp`:

- Framework preset: `Other`
- Root directory: `./`
- Build command: leave empty
- Output directory: leave empty
- Install command: leave empty

The repository contains `Dockerfile.vercel` and `.htaccess`. Vercel runs the
PHP application as a container on Fluid compute; Apache routes web pages and
`/api/*` endpoints through `api/index.php`.

## Supabase database values

In Supabase, open **Project Settings → Database → Connect** and choose the
**Session pooler** connection. Use its host, port `5432`, database `postgres`,
user, and password in Vercel. Do not use the direct database host if the Vercel
runtime cannot reach it.

Create these Vercel environment variables for **Production, Preview, and
Development**:

```text
NANNYAPP_BASE_URL=https://YOUR-VERCEL-DOMAIN.vercel.app
NANNYAPP_DB_DRIVER=pgsql
NANNYAPP_DB_HOST=YOUR-SUPABASE-SESSION-POOLER-HOST
NANNYAPP_DB_PORT=5432
NANNYAPP_DB_NAME=postgres
NANNYAPP_DB_USER=YOUR-SUPABASE-POOLER-USER
NANNYAPP_DB_PASS=YOUR-SUPABASE-DATABASE-PASSWORD
NANNYAPP_DB_SSLMODE=require
NANNYAPP_MIGRATION_TOKEN=GENERATE-A-LONG-RANDOM-SECRET
NANNYAPP_SUPPORT_EMAIL=support@YOUR-VERCEL-DOMAIN.vercel.app
NANNYAPP_SUPPORT_PHONE=YOUR-SUPPORT-NUMBER
NANNYAPP_MAIL_TRANSPORT=mail
NANNYAPP_MAIL_FROM=Nanny-App <support@YOUR-VERCEL-DOMAIN.vercel.app>
NANNYAPP_STORAGE_DRIVER=database
NANNYAPP_STORAGE_QUOTA_BYTES=104857600
NANNYAPP_STORAGE_RESERVE_BYTES=20971520
```

Replace every `YOUR-...` value. Never commit these values or paste the
database password and storage secret into chat.

## Persistent uploads

For a small prototype, set `NANNYAPP_STORAGE_DRIVER=database` as above. When
`VERCEL` is set and no driver is specified, the application also defaults to
`database`. Uploads are stored as base64 in the `stored_media` table, so apply
migration v12 before testing uploads. The database driver rejects files larger
than 8 MiB and consumes database storage; monitor size and back up the database.
Hosted uploads are available only after the database connection, migration and
storage configuration work on the deployed host. Do not treat a passing code
build as proof that live uploads work.

For larger files or volume, configure persistent S3-compatible storage instead:
set `NANNYAPP_STORAGE_DRIVER=s3` and the applicable `NANNYAPP_S3_BUCKET`,
`NANNYAPP_S3_REGION`, `NANNYAPP_S3_ACCESS_KEY`, `NANNYAPP_S3_SECRET_KEY` and
`NANNYAPP_S3_ENDPOINT` values. Keep the bucket private and verify upload,
readback, deletion and backup on the live host. Changing drivers does not migrate
previously stored objects; plan that move separately.

## First deployment

1. Deploy the Vercel project once. The Vercel project root must be `./` so
   `Dockerfile.vercel` is detected.
2. Open `https://YOUR-VERCEL-DOMAIN.vercel.app/`.
3. While the migration token is temporarily enabled, run this from PowerShell:

   ```powershell
   Invoke-RestMethod -Method Post `
     -Uri "https://YOUR-VERCEL-DOMAIN.vercel.app/api/migrate.php" `
     -Headers @{ "X-Nanny-Migration-Token" = "THE-SAME-TOKEN" }
   ```

   It should return `{"ok":true,...}`. Then remove
   `NANNYAPP_MIGRATION_TOKEN` from Vercel and redeploy so the endpoint is
   disabled. The existing `NannyApp.Shared/bin/migrate.php` remains available
   for CLI/server deployments.
4. Confirm migration v12 created `stored_media` before testing database-backed
   uploads. Open `https://YOUR-VERCEL-DOMAIN.vercel.app/api/health.php`; it should now
   return an `ok` response.
5. Test registration, login, nanny profile upload, booking, messaging, and the
   Android API before stopping the AWS instance.

## Android release URL

After Vercel gives the project its final HTTPS URL, build the APK with:

```powershell
./gradlew.bat :app:assembleRelease -PNANNYAPP_RELEASE_API_URL=https://YOUR-VERCEL-DOMAIN.vercel.app/api/
```

The URL must end with `/api/`. Update the same value in the GitHub Actions
release environment when publishing the APK.

## GitHub automation

GitHub `main` protection requires reviewed pull requests and
strict CI checks; see [Automation](AUTOMATION.md) for the administrator recovery
exception. Disable automatic Vercel
Production deployments from `main`; promote a verified deployment only after a
release decision. Vercel settings are outside this repository, so verify this
configuration in the project. Keep preview data and credentials separate.

`Supabase database sync` runs only by manual dispatch from `main`, after
the protected `production-db` environment. Merging a migration does not run it; configure an environment reviewer separately if needed.
Back up and restore-test production data before dispatch. This workflow also
repairs demo data; use the demo password secret only while demo accounts are
intended.

Set these GitHub repository values before using that workflow:

- Variable `NANNYAPP_PRODUCTION_URL`: `https://YOUR-VERCEL-DOMAIN.vercel.app`
- Secret `NANNYAPP_DB_URL`: the Supabase database connection string
- Secret `NANNYAPP_DEMO_PASSWORD`: private demo password while demo accounts are needed

Run it from **GitHub → Actions → Supabase database sync → Run workflow**, selecting
`main`. The separate HTTP migration procedure above uses
`NANNYAPP_MIGRATION_TOKEN`; remove that token from both GitHub and Vercel after
its temporary use.
