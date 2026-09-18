# NannyApp test suite

This folder is the launch test home for the web portal, PHP API, and native Android app.

## Before every staging deployment

1. Run the Android unit tests:
   `./gradlew testDebugUnitTest`
2. Run the Android release build:
   `./gradlew assembleRelease`
3. Execute the [launch smoke checklist](manual/launch-smoke-checklist.md) against staging.
4. Record the browser/device, API URL, tester, and result for every failed check.

Do not use production accounts, payment details, or identity documents during testing.

## Test layers

- `manual/` — cross-product journeys that require a real device or browser.
- `api/` — endpoint contract and security cases to automate when staging credentials are available.
- `NannyApp.Mobile/NannyApp/app/src/test/` — fast Kotlin unit tests run by Gradle.

