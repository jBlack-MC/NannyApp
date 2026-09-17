<p align="center">
  <img src="../logo.jpeg" alt="Nanny-App logo" width="200">
</p>

# NannyApp.Shared

Holds everything that's common to **every** Nanny-App client:

- `NannyApp.Web` — the PHP web app (and its Cordova/PWA packaging, which is
  just that same site in a WebView — see `NannyApp.Web/apk/`).
- `NannyApp.Mobile/NannyApp` — the native Kotlin/Compose Android app and its
  PHP REST API layer (`api/`).

Today that's:

- `database/` — the single source of truth for the MySQL schema
  (`schema.sql`) plus incremental migrations (`migrate_v2/3/4/5_api.sql`,
  `phase1_constraints.sql`, `phase2_authentication.sql`, `seed_reviews.sql`).
  Import `database/schema.sql` first, then run the numbered migrations in
  order (`NannyApp.Web/migrate_v2.php` etc., or `mysql -u root nanny_app <
  database/migrate_v2.sql` directly).
- `config/database.php` — defines the `DB_*` constants and the `db()` PDO
  singleton. Each client's own `config/database.php` requires this file when
  it's present (falling back to a fully standalone connection otherwise), so
  every client talks to the exact same MySQL database — nothing app-specific
  is stored per project.
- `storage/uploads/` — where uploaded profile photos, nanny gallery photos,
  banners and verification documents actually live on disk **in local mode**.
  This sits outside any client's webroot on purpose: `NannyApp.Web/media.php`
  and `NannyApp.Mobile/NannyApp/api/media.php` both stream files from here by
  relative path (`media.php?f=uploads/nannies/2026/09/ab12cd34.jpg`),
  validating the path stays inside `storage/` before serving it. Only the
  small path stored in the database (e.g. `users.profile_image`) needs to be
  duplicated — the file itself is never copied per app. `.htaccess` here
  blocks direct browser access, since PHP reads these files from disk, not
  over HTTP.
- `config/storage.php` — the upload storage abstraction both clients call
  through `save_uploaded_image()` / `media_url()`. Two drivers, switched with
  **no code changes**:
  - `local` (default) — files sharded into `<subdir>/<year>/<month>/`
    folders under `storage/uploads/` so no single folder ever grows
    unbounded. Fine for development or a single-server deployment.
  - `s3` — any S3-compatible object storage (AWS S3, Cloudflare R2,
    DigitalOcean Spaces, MinIO...). Files never touch local disk, so it
    works across any number of stateless app instances/containers and
    doesn't fill up server disk as usage grows. **Use this driver in
    production.** Enable it with environment variables (never commit
    secrets to the repo):

    ```bash
    NANNYAPP_STORAGE_DRIVER=s3
    NANNYAPP_S3_BUCKET=nannyapp-uploads
    NANNYAPP_S3_REGION=us-east-1
    NANNYAPP_S3_ACCESS_KEY=...
    NANNYAPP_S3_SECRET_KEY=...
    NANNYAPP_S3_ENDPOINT=            # leave blank for real AWS S3; set for R2/Spaces/MinIO
    NANNYAPP_S3_PUBLIC_BASE_URL=     # set if the bucket/CDN serves objects publicly (skips presigning)
    ```

**Working standalone.** Every client also works if `NannyApp.Shared` isn't
checked out next to it (e.g. one project zipped up alone): each
`config/database.php` checks whether the shared file exists and only then
requires it — otherwise it falls back to its own local `DB_*`
constants/`db()`/upload folder (local storage driver only; standalone mode
has no shared config to read S3 credentials from). No code changes needed
either way.

Adding a second client in the future just means pointing its config at this
folder instead of re-implementing DB access or file storage.

## Going to production

- Set the `NANNYAPP_S3_*` env vars above so uploads go to object storage
  instead of local disk — required if you deploy to more than one app
  instance/container, or to any host with ephemeral/non-shared disk.
- Set real `DB_*` values via env vars or by editing `config/db_credentials.php`
  (Mobile) / `NannyApp.Web/config/config.php` (Web) — never commit production
  DB credentials.
- Keep `NannyApp.Shared/.htaccess` (or equivalent web-server rule) in place so
  this folder is never served directly if it ends up under a public docroot.
