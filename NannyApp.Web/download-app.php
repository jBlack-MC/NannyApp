<?php
/** Public Android download endpoint. Keep signed releases outside the web root. */
require_once __DIR__ . '/config/config.php';

$apkPath = getenv('NANNYAPP_APK_PATH') ?: dirname(__DIR__) . '/NannyApp.Shared/releases/NannyApp-latest.apk';
$apkPath = is_string($apkPath) ? $apkPath : '';

if (is_file($apkPath) && is_readable($apkPath)) {
    header('Content-Type: application/vnd.android.package-archive');
    header('Content-Length: ' . (string) filesize($apkPath));
    header('Content-Disposition: attachment; filename="NannyApp-Android.apk"');
    header('X-Content-Type-Options: nosniff');
    header('Cache-Control: private, no-store');
    readfile($apkPath);
    exit;
}

http_response_code(503);
$pageTitle = 'Android app download';
require __DIR__ . '/includes/header.php';
?>
<section class="page section-pad">
    <div class="container" style="max-width:640px">
        <article class="card download-app-card">
            <p class="h-eyebrow">Android app</p>
            <h1>The app download is not available yet.</h1>
            <p class="muted">Please use the web portal for now. A signed Android release will be published here once it is ready.</p>
            <a class="btn btn-primary" href="<?= url('index.php') ?>">Return home</a>
        </article>
    </div>
</section>
<?php require __DIR__ . '/includes/footer.php'; ?>
