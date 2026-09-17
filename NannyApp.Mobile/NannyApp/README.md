<p align="center">
  <img src="../../logo.jpeg" alt="Nanny-App logo" width="200">
</p>

# NannyApp — Native Android App + PHP/MySQL API

A native Kotlin/Jetpack Compose Android app for the NannyApp childcare marketplace,
rebuilt from the original PHP/MySQL web app (`nannyapp-master.zip`), plus a REST API
layer (`api/`) that sits between the app and the existing MySQL database.

```
Android App (Kotlin, Compose, MVVM)
        │  Retrofit/HTTPS + Bearer token
        ▼
PHP REST API  (this repo's api/ folder)
        │  PDO / prepared statements
        ▼
MySQL  (nanny_app — same schema as the original web app, plus one new table)
```

## What's included

- **`app/`** — the full Android Studio project (Kotlin, Jetpack Compose, Material 3, MVVM + Repository pattern, Hilt DI, Room cache, Retrofit, DataStore session storage, Navigation Compose with role-based bottom navigation for Parent / Nanny / Admin).
- **`api/`** — PHP REST endpoints (JSON) that reuse the original app's `config/database.php` and `includes/functions.php` business logic (booking conflict checks, check-in PIN generation, escrow release, rating recomputation, chat message rate limiting, etc.) rather than reimplementing it. `api/media.php` streams uploaded avatars/portfolio files.
- **`config/`**, **`includes/`** — DB connection + helper functions. `config/database.php` automatically uses `../../NannyApp.Shared/config/database.php` (same MySQL database + uploaded files as the web app) when `NannyApp.Shared` is checked out next to this project, and falls back to a fully standalone connection (using `config/db_credentials.php`) otherwise — no code changes needed either way.
- **`backend_docs/API.md`** — full endpoint reference.

The database schema itself now lives in **`../../NannyApp.Shared/database/`** (shared with the web app): `schema.sql`, `migrate_v2..v4.sql`, `phase1/phase2` files, and `migrate_v5_api.sql` (adds the `api_tokens` table this API uses for bearer-token auth, since the original app uses PHP sessions which don't suit a native mobile client).

## 1. Database setup

```bash
mysql -u root < ../../NannyApp.Shared/database/schema.sql
mysql -u root nanny_app < ../../NannyApp.Shared/database/migrate_v2.sql
mysql -u root nanny_app < ../../NannyApp.Shared/database/migrate_v3.sql
mysql -u root nanny_app < ../../NannyApp.Shared/database/migrate_v4.sql
mysql -u root nanny_app < ../../NannyApp.Shared/database/migrate_v5_api.sql   # api_tokens table
# phase1_constraints.sql / phase2_authentication.sql / seed_reviews.sql are optional extras from the original project
```

Running this project without `NannyApp.Shared` present? Grab the same files directly — they aren't API-specific (except `migrate_v5_api.sql`, which is).

## 2. Backend setup (PHP)

Requires PHP 8.1+ with the `pdo_mysql` and `curl` extensions (standard in XAMPP/MAMP/most hosts).

1. Copy this whole project (or just `api/`, `config/`, `includes/`, `database/`) to your web server, e.g. `htdocs/nannyapp/`.
2. `cp config/db_credentials.example.php config/db_credentials.php` and fill in your MySQL host/user/password.
3. `cp config/paystack.example.php config/paystack.php` and fill in your Paystack secret/public keys (test keys are fine for development — get them from your Paystack dashboard).
4. Make sure the upload storage location is writable by PHP: with `NannyApp.Shared` present that's `../../NannyApp.Shared/storage/` (shared with the web app); standalone it's this project's own `assets/` folder (`chmod 775 assets`). For production, set `NANNYAPP_STORAGE_DRIVER=s3` (plus the `NANNYAPP_S3_*` env vars) so uploads go straight to S3-compatible object storage instead of local disk — see `NannyApp.Shared/README.md`.
5. Confirm the API responds: `curl -X POST http://localhost/nannyapp/api/auth/login.php -d '{"email":"parent@nanny.app","password":"Password123!","rememberMe":true}' -H 'Content-Type: application/json'` should return a JSON envelope with a `token`.

No PHP built-in server or MySQL was available in the environment that generated this project, so the API has been written carefully against the schema but **has not been executed end-to-end** — please run it locally and fix anything that surfaces before shipping.

## 3. Android Studio setup

1. Open the `app/` project root (the folder containing `settings.gradle.kts`) in Android Studio (Hedgehog/2023.1+ recommended).
2. Let Gradle sync — it will download Compose BOM 2024.06.00, Hilt 2.51.1, Room 2.6.1, Retrofit 2.11.0, etc. (see `app/build.gradle.kts` for the full list).
3. Point the app at your backend: `app/build.gradle.kts` sets `API_BASE_URL` per build type —
   - `debug` defaults to `http://10.0.2.2/nannyapp/api/` (the Android emulator's alias for your host machine's `localhost`, works with XAMPP out of the box).
   - `release` defaults to a placeholder HTTPS URL — change it before shipping.
   - For a physical device on the same Wi-Fi, use your computer's LAN IP instead of `10.0.2.2`.
4. Run on an emulator or device (min SDK 24, target/compile SDK 34).

This sandbox also has no Android SDK/Gradle/Kotlin toolchain, so `./gradlew build` was not run here — please run a Gradle sync and build locally as the first step and report back anything that doesn't compile so it can be fixed.

## 4. Demo credentials

All seeded accounts use the password **`Password123!`**:

| Role | Email |
|---|---|
| Admin | `admin@nanny.app` |
| Parent | `parent@nanny.app` |
| Parent | `james@nanny.app` |
| Nanny (verified) | `amelia@nanny.app` |
| Nanny (verified) | `margaret@nanny.app` |
| Nanny (pending verification) | `jasmine@nanny.app` |

## 5. Environment variables / secrets

| Where | What |
|---|---|
| `config/db_credentials.php` | `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASS` |
| `config/paystack.php` | `PAYSTACK_SECRET_KEY`, `PAYSTACK_PUBLIC_KEY` — secret key **never** leaves the server |
| `app/build.gradle.kts` | `API_BASE_URL` per build type |

## 6. Project structure (Android)

```
com.nannyapp
├── data
│   ├── api          Retrofit interfaces + DTOs
│   ├── db            Room entities/DAOs/database (local cache only)
│   ├── model         (unused placeholder — domain models live in domain.model)
│   ├── repository    Repository implementations (network + cache)
│   └── preferences   SessionManager (DataStore)
├── di                Hilt modules (Network, Database, Repository)
├── domain
│   ├── model         Pure Kotlin domain models + enums matching the SQL schema
│   ├── repository    Repository interfaces consumed by ViewModels
│   └── usecase       (reserved for future use-case extraction)
├── ui
│   ├── navigation    Routes + role-scoped NavHost shells (Parent/Nanny/Admin)
│   ├── components    PrimaryButton, NannyCard, BookingCard, StatCard, RatingView,
│   │                 VerificationBadge, LoadingView/EmptyState/ErrorState, AppTopBar, BottomNav
│   ├── theme         Color/Type/Shape/Theme (Material 3)
│   ├── auth          Splash, Welcome, Login, Register, Forgot password
│   ├── parent        Dashboard, Find Nannies, Nanny Detail, Saved, Children, Bookings, Payments
│   ├── nanny         Dashboard, Bookings, Availability, Earnings, Reviews, Profile edit, Portfolio
│   ├── booking       Booking wizard, Booking detail, Check-in, Leave review, Payment WebView
│   ├── admin         Dashboard, Users, Verifications, Bookings, Payments, Reports, Support
│   ├── messaging     Conversation list, Chat (polling)
│   ├── notifications Notifications list
│   ├── profile       Account/profile + password change + logout
│   ├── support       Ticket list + new ticket
│   └── misc          CMS static-page viewer (About/FAQ/Terms/etc.)
└── MainActivity.kt
```

## 7. Honest notes on scope

This is a large brief (40+ requested screens/behaviors across three roles plus a full API layer). What's here is a genuinely wired, non-mocked implementation — every screen has a real ViewModel calling a real Repository calling a real Retrofit API against the endpoints documented in `backend_docs/API.md`, with Room caching, error/loading/empty states, and role-based navigation throughout. A few things to be upfront about:

- **Not compiled here.** No Android SDK/Gradle/Kotlin compiler or PHP/MySQL runtime existed in the environment that produced this project, so neither side has been build-verified end to end. Please run a Gradle sync/build and hit the API against a real database as your first steps, and treat this as a strong, thorough first draft rather than a guaranteed zero-error build.
- **Original web app admin-web-only rule overridden.** The original PHP explicitly blocks admin accounts from the packaged/native app (`is_native_app_request()` in `config/config.php`). Your brief explicitly asked for a native Admin module, so this project implements one and documents the conflict rather than silently dropping either requirement.
- **Image assets** from `assets/img/` in the original zip were not migrated into Android drawables — the app uses Coil to load `photo_url`/`banner_image` values (and a couple of stock Unsplash placeholders on the Welcome screen) rather than bundling the original binary assets.
- **Portfolio/ID uploads** reuse the original `save_uploaded_image()` helper, which only accepts image formats — extend it if you need PDF support for ID documents. File picking itself (`rememberFilePickerLauncher` in `ui/components/FilePicker.kt`) is fully wired end to end for both portfolio uploads and profile photo changes.
- **Email verification** has a dedicated screen (`VerifyEmailScreen`) reachable from Register, from Login's "unverified" error, and via an `nannyapp://verify-email?token=...` deep link (registered in the manifest) for a real email link to open. Wiring the backend to actually *send* that email is still a `// TODO` in `api/auth/register.php`/`forgot.php` — see `backend_docs/API.md`.
- **Background sync**: `NotificationSyncWorker` (Hilt + WorkManager) refreshes notifications every 15 minutes while logged in, so the badge count doesn't go stale when the app isn't in the foreground — this is the one place WorkManager is actually exercised, per the technology requirements.
