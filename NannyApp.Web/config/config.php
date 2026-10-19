<?php
/**
 * Global application configuration.
 * Adjust BASE_URL if you deploy the app under a different path/host.
 */

declare(strict_types=1);

header('Cache-Control: private, no-store');
header('Referrer-Policy: no-referrer');

// --- App ---------------------------------------------------------------
define('APP_NAME', getenv('NANNYAPP_NAME') ?: 'Nanny-App');
define('SUPPORT_EMAIL', getenv('NANNYAPP_SUPPORT_EMAIL') ?: 'support@example.invalid');
define('SUPPORT_PHONE', getenv('NANNYAPP_SUPPORT_PHONE') ?: 'Not configured');
define('APP_TAGLINE', 'Care • Connect • Comfort');

// Base URL of the app (folder under htdocs). No trailing slash. Vercel
// provides VERCEL_URL without a scheme, so use it as a safe preview fallback
// when an explicit production URL has not yet been configured.
$configuredBaseUrl = trim((string) (getenv('NANNYAPP_BASE_URL') ?: ''));
if ($configuredBaseUrl === '' && ($vercelUrl = trim((string) (getenv('VERCEL_URL') ?: ''))) !== '') {
    $configuredBaseUrl = 'https://' . $vercelUrl;
}
define('BASE_URL', rtrim($configuredBaseUrl !== '' ? $configuredBaseUrl : '/nannyapp', '/'));

// --- Database (XAMPP defaults / Supabase PostgreSQL) ------------------
// One rule for the website and the API: a full connection URL (NANNYAPP_DB_URL, else the
// Supabase integration's POSTGRES_URL_NON_POOLING) wins as a whole. See db_settings.php.
require_once __DIR__ . '/../../NannyApp.Shared/config/db_settings.php';
nannyapp_define_db_constants();

// --- Error reporting (off in production; errors go to server log only) ---
error_reporting(E_ALL);
ini_set('display_errors', '0');
ini_set('log_errors', '1');

// Uncaught errors are logged with a reference the visitor can quote to support.
require_once __DIR__ . '/../../NannyApp.Shared/config/logging.php';
install_error_handlers('web');

// --- HTTPS enforcement ---
function is_https_request(): bool
{
    $forwardedProto = $_SERVER['HTTP_X_FORWARDED_PROTO'] ?? '';
    if (is_string($forwardedProto) && strtolower($forwardedProto) === 'https') {
        return true;
    }

    return !empty($_SERVER['HTTPS']) && strtolower((string) $_SERVER['HTTPS']) !== 'off';
}

if (!is_https_request() && $_SERVER['SERVER_NAME'] !== 'localhost' && $_SERVER['SERVER_NAME'] !== '127.0.0.1') {
    header('Location: https://' . $_SERVER['HTTP_HOST'] . $_SERVER['REQUEST_URI']);
    exit;
}

// --- Session (secure cookie flags before session_start) ---------------
if (session_status() === PHP_SESSION_NONE) {
    ini_set('session.cookie_httponly', '1');
    ini_set('session.use_strict_mode', '1');
    ini_set('session.cookie_samesite', 'Lax');
    
    // HTTPS only in production
    if (is_https_request() || ($_SERVER['SERVER_NAME'] !== 'localhost' && $_SERVER['SERVER_NAME'] !== '127.0.0.1')) {
        ini_set('session.cookie_secure', '1');
    }

    // Serverless hosts keep sessions in the database instead of each instance's own disk.
    require_once __DIR__ . '/../../NannyApp.Shared/config/session.php';
    nannyapp_configure_sessions();
    session_start();
}

require_once __DIR__ . '/database.php';
require_once __DIR__ . '/../includes/functions.php';
require_once __DIR__ . '/../includes/email.php';

// --- Admin is web-only: kill any admin session that's somehow active
//     inside the packaged app / installed PWA (e.g. cookies carried over
//     from a browser session before the PWA was installed). ------------
if (!empty($_SESSION['user_id']) && is_native_app_request()) {
    $sessionUser = current_user();
    if ($sessionUser && $sessionUser['role'] === 'admin') {
        $_SESSION = [];
        session_destroy();
        session_start();
        flash('Admin accounts can only sign in from a web browser — not the app.', 'error');
    }
}
