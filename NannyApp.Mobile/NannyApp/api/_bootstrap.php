<?php
/**
 * Shared bootstrap for every /api/*.php endpoint.
 *
 * Reuses the existing app's config/database.php (db() PDO singleton) and
 * includes/functions.php (notify(), recompute_rating(), generate_check_in_code(),
 * nanny_has_booking_conflict(), send_chat_message(), create_support_ticket(),
 * auto_release_stale_payments()) so business logic is not duplicated between
 * the web app and this API layer — both read/write the same MySQL database.
 *
 * Every endpoint under /api/ must start with:
 *     require_once __DIR__ . '/../_bootstrap.php';
 * (adjust the relative path depth for files nested one level deeper, e.g. /api/auth/login.php)
 */

declare(strict_types=1);

error_reporting(E_ALL);
ini_set('display_errors', '0');
ini_set('log_errors', '1');

header('Content-Type: application/json; charset=utf-8');
header('X-Content-Type-Options: nosniff');

// is_rate_limited()/increment_rate_limit() (reused from includes/functions.php)
// store their counters in $_SESSION. Start a session so those calls don't warn;
// note that since the Android client doesn't carry a session cookie between
// requests, this rate limiting is best-effort per-request rather than truly
// persistent — for production, swap those two helpers for a database- or
// Redis-backed limiter keyed by IP.
if (session_status() === PHP_SESSION_NONE) {
    @session_start();
}

// CORS is not needed for the native app (no browser origin), but harmless to allow same-origin tooling.
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Authorization, Content-Type');
if (($_SERVER['REQUEST_METHOD'] ?? '') === 'OPTIONS') {
    http_response_code(204);
    exit;
}

// Mark this request as coming from the API layer so functions.php's
// is_native_app_request() (which normally blocks admin sessions from the
// packaged web PWA) does not also reject legitimate admin API calls —
// API auth uses bearer tokens, not the session cookie, so that check does
// not apply here.
define('NANNYAPP_API_REQUEST', true);

require_once __DIR__ . '/../config/db_credentials.php';
require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../includes/functions.php';

// Optional: Paystack keys live outside version control. See config/paystack.example.php.
if (file_exists(__DIR__ . '/../config/paystack.php')) {
    require_once __DIR__ . '/../config/paystack.php';
}

const API_TOKEN_TTL_DAYS = 90;

function json_response(bool $success, mixed $data = null, ?string $message = null, int $httpCode = 200): never
{
    http_response_code($httpCode);
    echo json_encode(['success' => $success, 'message' => $message, 'data' => $data], JSON_UNESCAPED_SLASHES);
    exit;
}

function json_error(string $message, int $httpCode = 400): never
{
    json_response(false, null, $message, $httpCode);
}

function json_body(): array
{
    $raw = file_get_contents('php://input');
    if ($raw === false || $raw === '') {
        return [];
    }
    $decoded = json_decode($raw, true);
    return is_array($decoded) ? $decoded : [];
}

/** Generates an opaque bearer token, storing only its SHA-256 hash (never the raw value). */
function issue_api_token(int $userId, ?string $deviceInfo = null): string
{
    $raw = bin2hex(random_bytes(32));
    $hash = hash('sha256', $raw);
    $expires = (new DateTime())->modify('+' . API_TOKEN_TTL_DAYS . ' days')->format('Y-m-d H:i:s');

    $stmt = db()->prepare(
        'INSERT INTO api_tokens (user_id, token_hash, device_info, expires_at) VALUES (:uid, :hash, :device, :expires)'
    );
    $stmt->execute(['uid' => $userId, 'hash' => $hash, 'device' => $deviceInfo, 'expires' => $expires]);

    return $raw;
}

/** Reads the Authorization: Bearer <token> header and resolves it to a users row, or null. */
function authenticated_user(): ?array
{
    static $cached = null;
    static $resolved = false;
    if ($resolved) {
        return $cached;
    }
    $resolved = true;

    $header = $_SERVER['HTTP_AUTHORIZATION'] ?? ($_SERVER['REDIRECT_HTTP_AUTHORIZATION'] ?? '');
    if (!preg_match('/Bearer\s+(\S+)/i', $header, $m)) {
        return null;
    }
    $hash = hash('sha256', $m[1]);

    $stmt = db()->prepare(
        'SELECT u.* FROM api_tokens t JOIN users u ON u.id = t.user_id
         WHERE t.token_hash = :hash AND t.expires_at > NOW() LIMIT 1'
    );
    $stmt->execute(['hash' => $hash]);
    $user = $stmt->fetch();

    if ($user) {
        db()->prepare('UPDATE api_tokens SET last_used_at = NOW() WHERE token_hash = :hash')->execute(['hash' => $hash]);
        $cached = $user;
    }
    return $cached;
}

/** Aborts with 401 unless a valid bearer token is present; returns the users row. */
function require_api_auth(): array
{
    $user = authenticated_user();
    if (!$user) {
        json_error('Your session has expired. Please log in again.', 401);
    }
    if ($user['status'] === 'suspended') {
        json_error('Your account has been suspended. Contact support for help.', 403);
    }
    return $user;
}

/** Aborts with 403 unless the authenticated user has one of the given roles. */
function require_api_role(array $user, string ...$roles): void
{
    if (!in_array($user['role'], $roles, true)) {
        json_error('You are not authorized to perform this action.', 403);
    }
}

function generate_booking_ref(int $bookingId): string
{
    return 'BK' . str_pad((string) $bookingId, 6, '0', STR_PAD_LEFT);
}
