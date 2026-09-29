<?php
// Only for the disposable regression server. Never deploy this router.
if (!preg_match('/^nanny_(?:security|operations)_test_[a-f0-9]{12}$/', getenv('NANNYAPP_DB_NAME') ?: '')) exit;
define('SHARED_STORAGE_DIR', getenv('NANNYAPP_TEST_STORAGE'));
if (($_GET['test_driver'] ?? '') === 's3') {
    define('STORAGE_DRIVER', 's3');
    define('S3_BUCKET', 'synthetic-private');
    define('S3_ACCESS_KEY', 'synthetic-key');
    define('S3_SECRET_KEY', 'synthetic-secret');
    define('S3_ENDPOINT', 'https://synthetic-s3.example.invalid');
    define('S3_PUBLIC_BASE_URL', 'https://synthetic-public.example.invalid');
}
$root = dirname(__DIR__, 2);
$path = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);
if ($path === '/ready') { echo 'ready'; return; }
if ($path === '/session') {
    require $root . '/NannyApp.Web/config/config.php';
    $stmt = db()->prepare('SELECT * FROM users WHERE id = ?');
    $stmt->execute([(int) ($_GET['id'] ?? 0)]);
    $user = $stmt->fetch();
    $_SESSION['user_id'] = $user['id'];
    $_SESSION['auth_password'] = hash('sha256', $user['password_hash']);
    if (isset($_GET['details'])) echo json_encode(['session'=>session_id(),'csrf'=>csrf_token()]);
    else echo session_id();
    return;
}
$base = str_starts_with($path, '/api/') ? $root . '/NannyApp.Mobile/NannyApp/api' : $root . '/NannyApp.Web';
$relative = preg_replace('#^/(api|web)/#', '', $path);
$file = realpath($base . '/' . $relative);
if (!$file || !str_starts_with($file, realpath($base) . DIRECTORY_SEPARATOR) || pathinfo($file, PATHINFO_EXTENSION) !== 'php') { http_response_code(404); return; }
require $file;
