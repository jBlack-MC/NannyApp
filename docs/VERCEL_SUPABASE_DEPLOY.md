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

The repository contains `vercel.json`, which routes the PHP web pages and
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
NANNYAPP_STORAGE_DRIVER=s3
NANNYAPP_S3_BUCKET=nannyapp-media
NANNYAPP_S3_REGION=eu-west-1
NANNYAPP_S3_ACCESS_KEY=YOUR-SUPABASE-STORAGE-S3-ACCESS-KEY
NANNYAPP_S3_SECRET_KEY=YOUR-SUPABASE-STORAGE-S3-SECRET-KEY
NANNYAPP_S3_ENDPOINT=https://qvxhpjnnuwygpgcooqil.storage.supabase.co/storage/v1/s3
NANNYAPP_S3_PUBLIC_BASE_URL=
NANNYAPP_STORAGE_QUOTA_BYTES=104857600
NANNYAPP_STORAGE_RESERVE_BYTES=20971520
```

Replace every `YOUR-...` value. Never commit these values or paste the
database password and storage secret into chat.

## Supabase Storage

In Supabase open **Storage → New bucket**, create a private bucket named
`nannyapp-media`. Then open **Storage → S3 Access Keys** and create an access
key. Put the key and secret into the two Vercel variables above. The bucket is
private so application-generated signed URLs remain protected.

## First deployment

1. Deploy the Vercel project once.
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
4. Open `https://YOUR-VERCEL-DOMAIN.vercel.app/api/health.php`; it should now
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
