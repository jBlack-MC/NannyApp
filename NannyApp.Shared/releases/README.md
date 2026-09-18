# Android release hand-off

The signed file served by `NannyApp.Web/download-app.php` is expected at
`NannyApp-latest.apk` in this directory for local/shared deployments, or at the
private path specified by `NANNYAPP_APK_PATH` in production.

Do not commit APKs, keystores, or signing passwords. For each release, record
outside the repository:

- version name and version code;
- SHA-256 checksum of the signed APK;
- signing-key owner and recovery location;
- staging test result and release date.

The website serves the file as an attachment and this directory remains outside
the web project’s public asset folder.
