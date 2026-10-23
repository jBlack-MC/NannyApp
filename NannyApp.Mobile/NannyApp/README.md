# Android app and PHP JSON API

Kotlin/Jetpack Compose with Hilt, Retrofit, Room and DataStore. The API uses the required shared PHP services and the same database as the website. Read [API contracts](backend_docs/API.md) alongside the actual endpoint code.

## Start the backend

Use [local Docker setup](../../docs/LOCAL_DEVELOPMENT.md) from the repository root. It serves both website and API. For deployment, keep `NannyApp.Shared` adjacent and follow [Vercel + Supabase](../../docs/VERCEL_SUPABASE_DEPLOY.md). Apply migrations with the shared runner; authentication and integrity migrations are required, not optional extras. Payments currently use a manual ledger; Paystack credentials are not required for the pilot.

## Build Android

Open this directory in Android Studio. CI uses JDK 21; Gradle currently declares compile SDK 36, target SDK 35 and minimum SDK 26. Dependency and version values in `app/build.gradle.kts` are authoritative.

```sh
./gradlew --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

On Windows use `gradlew.bat`. Debug defaults to `http://10.0.2.2/nannyapp/api/` for the emulator. For a physical phone, use a reachable LAN or HTTPS server:

```sh
./gradlew :app:assembleDebug -PNANNYAPP_DEBUG_API_URL=http://192.168.1.20/nannyapp/api/
```

Release builds require an explicit HTTPS API URL:

```sh
./gradlew :app:assembleRelease -PNANNYAPP_RELEASE_API_URL=https://nannyapp-two.vercel.app/api/
```

A release build without signing configuration is not a published installable release. Use the protected [Android release workflow](../../.github/workflows/android-release.yml) and [CI/CD guide](../../docs/CI_CD.md) for signing, tagging and endpoint validation. Never ship the CI synthetic endpoint. Increase the Android version code for each update and retain the signing key.

## Login and shared data

Use the private password issued for each hosted account. `Password123!` works only with local fixtures; see [demo access](../../README.md#sign-in-and-demo-accounts). Admin login is deliberately rejected by the mobile API. Do not put database passwords, service keys or demo passwords into the APK.

The same parent/nanny account works on web and Android. The server is authoritative for bookings, profiles and messages. Refresh/polling retrieves changes; offline caches are account-scoped and do not guarantee real-time synchronization. Session changes clear private cached data. Password changes and logout invalidate other sessions on their next authorized request.

## Content, theme and tests

Public welcome content is bundled for offline use, then refreshed from the server. After changing that content, run `python tests/build-public-content.py` from the repository root before rebuilding. The app supports system, light and dark themes; see [contrast guidance](../../docs/THEME_AND_CONTRAST.md).

Run the MariaDB and PostgreSQL Docker suites from the root for API/web registration, permissions and shared-data tests. JVM tests and lint run in CI. Physical-device navigation, upgrades, offline transitions and external-provider acceptance still require the [manual smoke checklist](../../tests/manual/launch-smoke-checklist.md).
