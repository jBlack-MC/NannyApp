# Android downloads

Place the tested, signed native Android release here as **NannyApp-latest.apk**.
The production compose configuration mounts this folder privately and connects
it to the website's **Download App** page automatically. Replacing the file
publishes the next download without rebuilding the server image.

Local development uses `NannyApp.Shared/releases/NannyApp-latest.apk` instead.
APKs and signing keys must stay out of Git. Build with the deployed HTTPS API URL
and retain the same signing key for updates.
