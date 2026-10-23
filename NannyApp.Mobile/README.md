# Native Android and PHP API

The Kotlin/Jetpack Compose app lives in [NannyApp](NannyApp/README.md). Open that directory, containing `settings.gradle.kts`, in Android Studio. Its `api/` directory is the PHP JSON backend deployed at `/api/` beside the website.

The API and website require `NannyApp.Shared` and share the same MariaDB or PostgreSQL database, account records and authorized uploads. Android connects over HTTP(S), not directly to the database. Parents and nannies use the native app; admins use a normal web browser.

[Setup and build commands](NannyApp/README.md) · [Demo access](../README.md#sign-in-and-demo-accounts) · [CI/CD](../docs/CI_CD.md)

Use the [website download](https://nannyapp-two.vercel.app/download-app.php?platform=android) for the published APK. Old local APKs may contain obsolete API URLs. `Password123!` is only a local fixture password, not the hosted demo password.
