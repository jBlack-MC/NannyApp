# Android release distribution

The hosted website can redirect its download route to the signed APK on GitHub Releases. For local/shared-file deployments, `NannyApp-latest.apk` in this directory is the fallback; production can instead set `NANNYAPP_APK_PATH` to a private file. See [CI/CD](../../docs/CI_CD.md) and the [download handler](../../NannyApp.Web/download-app.php).

Use the protected Android release workflow with a version tag already merged into main. It checks the embedded API URL and APK signature before publishing. Do not distribute old `app/release/` outputs or unsigned CI verification artifacts.

Record the APK's actual version name/code, SHA-256, signing-key owner, tested backend URL and physical-device acceptance result. A release tag alone does not establish the binary's version. Retain the signing key for updates and increase the version code.

APKs, keystores, passwords and private release records stay out of Git. Local release files remain outside the public web asset directory.
