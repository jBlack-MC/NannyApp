<?php
/** Public Android download endpoint. Keep signed releases outside the web root. */
require_once __DIR__ . '/config/config.php';

$apkPath = getenv('NANNYAPP_APK_PATH') ?: dirname(__DIR__) . '/NannyApp.Shared/releases/NannyApp-latest.apk';
$apkPath = is_string($apkPath) ? $apkPath : '';

$apkAvailable = is_file($apkPath) && is_readable($apkPath);
$downloadRequested = ($_GET['platform'] ?? '') === 'android';
if ($downloadRequested && $apkAvailable) {
    header('Content-Type: application/vnd.android.package-archive');
    header('Content-Length: ' . (string) filesize($apkPath));
    header('Content-Disposition: attachment; filename="NannyApp-Android.apk"');
    header('X-Content-Type-Options: nosniff');
    header('Cache-Control: private, no-store');
    readfile($apkPath);
    exit;
}

if ($downloadRequested) http_response_code(503);
$pageTitle = 'Get the app';
require __DIR__ . '/includes/header.php';
?>
<section class="section" aria-labelledby="install-title">
    <p class="h-eyebrow">Care, wherever you are</p>
    <h1 id="install-title">Nanny-App on your device</h1>
    <p class="muted">Use your existing account on your phone, tablet or computer. Choose how you want to get started.</p>
    <div class="install-grid">
        <article class="card">
            <h2>Android</h2>
            <p>Get the native app for Android 8.0 and newer.</p>
            <?php if ($apkAvailable): ?>
                <a class="btn btn-primary" href="<?= url('download-app.php?platform=android') ?>" download>Download Android app</a>
                <p class="muted">Your phone may ask permission to install from this browser. Only install the release downloaded from this site.</p>
            <?php else: ?>
                <p class="muted" role="status">The signed Android download is not published yet. You can install the web app below in the meantime.</p>
            <?php endif; ?>
        </article>
        <article class="card">
            <h2>iPhone &amp; iPad</h2>
            <p>Add Nanny-App to your Home Screen for an app window of its own.</p>
            <ol><li>Open this site in Safari.</li><li>Tap Share, then Add to Home Screen.</li><li>If shown, enable Open as Web App, then tap Add.</li></ol>
            <p class="muted">This is the web app. A native App Store version is not available yet.</p>
        </article>
        <article class="card">
            <h2>Web app &amp; computers</h2>
            <p>Install from a supported browser on Android, Windows, macOS or Linux, or continue using the website.</p>
            <button class="btn btn-primary" id="install-app" type="button" hidden>Install web app</button>
            <p id="install-status" class="muted" role="status">Look for Install app or Add to Home Screen in your browser menu. Available options depend on your browser.</p>
            <a class="btn" href="<?= url('index.php') ?>">Continue on the web</a>
        </article>
    </div>
    <p class="muted">An internet connection is needed to sign in, send messages and manage bookings. Installing does not make private account pages available offline.</p>
</section>
<script src="<?= url('assets/js/install.js') ?>" defer></script>
<?php require __DIR__ . '/includes/footer.php'; ?>
