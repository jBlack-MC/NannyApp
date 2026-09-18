# NannyApp.Mobile

![Nanny-App logo](NannyApp/assets/Icon_Logo.png)

The native mobile client for Nanny-App: a Kotlin/Jetpack Compose Android app,
plus the PHP REST API it talks to. This is a separate product from
`NannyApp.Web` (which is a PHP website, not a native app) — both read/write
the same MySQL database and the same uploaded files via `NannyApp.Shared`.

## What's in here

```text
NannyApp.Mobile/
└── NannyApp/          The actual project — open THIS folder in Android Studio
    ├── app/           Android Studio project (Kotlin, Compose, MVVM, Hilt, Room, Retrofit)
    ├── api/           PHP REST API (JSON) the app talks to over HTTPS
    ├── config/        DB connection + secrets (db_credentials.php, paystack.php — gitignored)
    ├── includes/      Shared PHP business-logic helpers used by api/
    ├── assets/        Local upload fallback folder (standalone mode only)
    └── backend_docs/  API.md — full endpoint reference
```text

See **[`NannyApp/README.md`](NannyApp/README.md)** for full setup instructions
(database, PHP backend, Android Studio) and the demo account list.

## How it relates to the rest of the repo

- `NannyApp/config/database.php` automatically reuses
  `NannyApp.Shared/config/database.php` (same MySQL database, same upload
  storage) when `NannyApp.Shared` is checked out next to this project, and
  falls back to a fully standalone connection otherwise — no code changes
  needed either way.
- The database schema itself lives in `NannyApp.Shared/database/` (this
  project only adds `migrate_v5_api.sql`, which creates the `api_tokens`
  table used for bearer-token auth, since the web app uses PHP sessions
  instead).
- Uploaded avatars/portfolio files go through the same storage abstraction
  as the web app (`NannyApp.Shared/config/storage.php`) — local disk in
  development, S3-compatible object storage in production.

See the [repo-wide overview](../README.md) for how all three projects fit
together.
