# Building the Nanny-App APK

The Nanny-App backend is **PHP + MySQL**, so the Android app is a thin native
**WebView wrapper** around the live site (the standard way to ship a PHP app as
an APK). Two supported routes — pick one.

> **Mobile UI source of truth:** the APK displays `NannyApp.Web`. Update the
> responsive PHP pages and CSS in that project for changes to the app landing
> page, navigation, and parent/nanny screens. `NannyApp.Mobile/NannyApp` is a
> separate native prototype and is not the UI packaged by the Cordova or
> Capacitor instructions below.
>
> **Admin is intentionally unavailable inside this app.** The server detects
> requests carrying the `NannyAppCordova` user-agent marker (set below via
> `AppendUserAgent`) or an installed-PWA cookie and blocks admin login/pages
> there, bouncing to `admin-unavailable.php` instead — admins must use a
> regular browser. See `is_native_app_request()` in `includes/functions.php`.
> **If you build with Route B (Capacitor) instead of the provided
> `config.xml`, you must add an equivalent user-agent override** (Capacitor's
> `appendUserAgent`/`overrideUserAgent` server option) or the server-side
> block won't recognize that build as the app.
>
> An APK cannot be produced from PHP/XAMPP alone — it requires the Android SDK,
> Java (JDK), and Gradle. Install **Android Studio** first (it bundles all three).

---

## Prerequisites (once)

1. Install **Android Studio** → during setup also install *Android SDK* and an
   *AVD* (virtual device) or enable USB debugging on a phone.
2. Install [**Node.js**](https://nodejs.org).
3. Install **JDK 17** (Android Studio bundles one; set `JAVA_HOME` to it).
4. Set env vars: `ANDROID_HOME` → your SDK path, and add `platform-tools` to PATH.

---

## Route A — Apache Cordova (uses the provided `config.xml`)

```bash
# 1. Install Cordova
npm install -g cordova

# 2. Create a project and enter it
cordova create nanny-apk app.nanny.knights Nanny-App
cd nanny-apk

# 3. Replace the generated config.xml with the one in this folder
#    (copy apk/config.xml over nanny-apk/config.xml)
#    Copy the favicon so it becomes the installer/launcher icon:
mkdir assets
mkdir assets/img
#    PowerShell: Copy-Item ..\NannyApp.Web\assets\img\icon.svg assets\img\icon.svg
#    macOS/Linux: cp ../NannyApp.Web/assets/img/icon.svg assets/img/icon.svg

# 4. Add the Android platform and build
cordova platform add android
cordova build android            # debug APK
# or a release (unsigned) APK:
# cordova build android --release
```

The APK is written to:
`nanny-apk/platforms/android/app/build/outputs/apk/debug/app-debug.apk`

## Publishing the download on the website

The public **Download App** link points to `download-app.php`. For a production
release, copy the signed APK to a private location and set `NANNYAPP_APK_PATH`
to that absolute file path (or use the local fallback
`NannyApp.Shared/releases/NannyApp-latest.apk`). The endpoint sends it with an
APK download header; do not put signed APKs under `assets/` or another browsable
web folder.

### GitHub Releases (recommended)

The included `.github/workflows/android-release.yml` builds the Android project
when you push a tag such as `v1.0.0` and attaches the APK to that GitHub
Release. Before using it, add these GitHub repository secrets: a base64-encoded
keystore named `NANNYAPP_RELEASE_KEYSTORE_BASE64`, plus
`NANNYAPP_RELEASE_KEYSTORE_PASSWORD`, `NANNYAPP_RELEASE_KEY_ALIAS` and
`NANNYAPP_RELEASE_KEY_PASSWORD`. Set the repository variable
`NANNYAPP_RELEASE_API_URL` if the app's production API URL is not
`https://nannyapp-two.vercel.app/api/`.

After the release exists, configure the PHP server with:

```text
NANNYAPP_APK_URL=https://github.com/jBlack-MC/NannyApp/releases/latest/download/NannyApp.apk
```

To create the base64 keystore value without putting the keystore in Git, run
the following from the directory containing the keystore and paste the output
into the GitHub repository secret named `NANNYAPP_RELEASE_KEYSTORE_BASE64`:

```bash
base64 -w 0 nanny.keystore
```

On PowerShell, use:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes('.\\nanny.keystore'))
```

The release workflow requires all four secrets before it starts Gradle:
`NANNYAPP_RELEASE_KEYSTORE_BASE64`, `NANNYAPP_RELEASE_KEYSTORE_PASSWORD`,
`NANNYAPP_RELEASE_KEY_ALIAS`, and `NANNYAPP_RELEASE_KEY_PASSWORD`. If any is
missing, the workflow stops with the secret name instead of an opaque shell
failure.

The previous `v1.0.3` run failed because these secrets were not configured. To
create a signing key once and save it in GitHub without committing it, run this
from PowerShell. Choose your own passwords and keep a backup of the keystore.

```powershell
keytool -genkeypair -v -keystore .\nanny-release.jks -alias nannyapp `
  -keyalg RSA -keysize 2048 -validity 10000 `
  -storepass "YOUR_KEYSTORE_PASSWORD" -keypass "YOUR_KEY_PASSWORD" `
  -dname "CN=Nanny-App, OU=Mobile, O=Nanny-App, L=Johannesburg, S=Gauteng, C=ZA"

$keystore64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes('.\nanny-release.jks'))
$keystore64 | gh secret set NANNYAPP_RELEASE_KEYSTORE_BASE64 --repo jBlack-MC/NannyApp
"YOUR_KEYSTORE_PASSWORD" | gh secret set NANNYAPP_RELEASE_KEYSTORE_PASSWORD --repo jBlack-MC/NannyApp
"nannyapp" | gh secret set NANNYAPP_RELEASE_KEY_ALIAS --repo jBlack-MC/NannyApp
"YOUR_KEY_PASSWORD" | gh secret set NANNYAPP_RELEASE_KEY_PASSWORD --repo jBlack-MC/NannyApp

# v1.0.3 already exists and can now be rebuilt.
gh workflow run android-release.yml --repo jBlack-MC/NannyApp -f tag=v1.0.3
gh run watch --repo jBlack-MC/NannyApp
gh release view v1.0.3 --repo jBlack-MC/NannyApp
```

Do not commit `nanny-release.jks`; add it to `.git/info/exclude` or delete it
after storing a secure backup. If a phone already has an APK signed with a
different key, uninstall that old APK once before installing this first signed
release. Future releases signed with this same key will update normally.

The web download button redirects to that release asset. Your existing APK can
also be uploaded manually as `NannyApp.apk` for the first release. Never commit
the APK or signing keystore to Git.

**Important — make the server reachable from the device:**

+ Start XAMPP (Apache + MySQL) on your PC.
+ Emulator: the host PC is `10.0.2.2`, so `config.xml` already points to
  `http://10.0.2.2/nannyapp/index.php`.
+ Real phone on the same Wi-Fi: change `content src` and `allow-navigation`
  to your PC's LAN IP, e.g. `http://192.168.1.20/nannyapp/index.php`
  (find it with `ipconfig`). Allow port 80 through Windows Firewall.

Run on a connected device/emulator directly:

```bash
cordova run android
```

---

## Route B — Capacitor (modern alternative)

```bash
npm init -y
npm install @capacitor/core @capacitor/cli @capacitor/android
npx cap init Nanny-App app.nanny.knights --web-dir=www
mkdir www && echo "redirect" > www/index.html   # placeholder shell
npx cap add android
# In android/app/src/main/AndroidManifest.xml allow cleartext (http) traffic,
# and set the server URL in capacitor.config.json:
#   "server": { "url": "http://10.0.2.2/nannyapp", "cleartext": true }
npx cap open android        # opens Android Studio -> Build > Build APK(s)
```

---

## Signing a release APK (for Play Store / sharing)

```bash
keytool -genkey -v -keystore nanny.keystore -alias nanny -keyalg RSA -keysize 2048 -validity 10000
# Build --release, then:
zipalign -v 4 app-release-unsigned.apk nanny-aligned.apk
apksigner sign --ks nanny.keystore --out nanny-app.apk nanny-aligned.apk
```

---

## Notes

+ The installer/launcher name is **Nanny-App**. The packaging icon and PWA
  icon both use the favicon `assets/img/icon.svg`; Android native builds use
  the equivalent vector drawable generated from the same artwork.
+ For production, host the PHP app on a real server with **HTTPS** and point
  `content src` at that URL — then no cleartext/IP juggling is needed.
+ The app is also an installable **PWA**: open the site in Chrome on Android →
  menu → *Install app / Add to Home screen*. That gives an app-like icon with
  no build step, useful for quick demos.
+ Rebuilding after a server-side update (new `config.xml` version, updated
  `service-worker.js` cache name) is enough to ship the change — the WebView
  always loads the live site, so most updates don't need a new APK at all.
  Only bump/rebuild the APK itself when `config.xml` (permissions, min SDK,
  splash, etc.) changes.

## Changelog

+ **1.1.0** — Admin dashboard now blocked inside the packaged app/installed
  PWA (web-browser-only by design). Escrow-style payment holds with a
  parent-issued check-in PIN before a nanny is paid out. Admin profile
  records.
+ **1.0.0** — Initial release.

## PowerShell release commands

Run these from the repository root. The release workflow requires the four
GitHub signing secrets and the `NANNYAPP_RELEASE_API_URL` repository variable.
Never commit the keystore or any secret values. Release tags are immutable in
practice, so use a new tag for every APK build; `v1.0.2` points to an older
commit and will not contain later fixes.

```powershell
# The current repair work is on this branch. Commit it first.
git switch fix/holiday-cache-on-main
git status
git add .github/workflows/ci.yml .github/workflows/android-release.yml `
  NannyApp.Mobile NannyApp.Shared NannyApp.Web docs scripts .env.production.example
git commit -m "Repair demo accounts and improve mobile release"
git push origin fix/holiday-cache-on-main

# Publish the same commit on main, which is the branch used for the website.
git switch main
git pull --ff-only origin main
git merge --no-ff fix/holiday-cache-on-main -m "Merge mobile release and database repair"
git push origin main

# This starts android-release.yml and publishes NannyApp.apk.
git tag -a v1.0.3 -m "Nanny-App Android release v1.0.3"
git push origin v1.0.3
```

Watch and verify the build:

```powershell
gh run watch
gh release view v1.0.3
```

If the tag already exists, run the workflow manually against that tag:

```powershell
gh workflow run "Android release" -f tag=v1.0.3
gh run watch
```

After the workflow is green, configure the web host with:

```text
NANNYAPP_APK_URL=https://github.com/jBlack-MC/NannyApp/releases/latest/download/NannyApp.apk
```

The CI workflow uploads unsigned verification APKs as workflow artifacts;
only the tag-controlled Android release workflow publishes a signed APK.
