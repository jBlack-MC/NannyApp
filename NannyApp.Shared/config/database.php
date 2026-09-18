<?php
/**
 * Single shared MySQL database for every Nanny-App client (web + packaged app).
 * Any project that needs the DB should require this file and call db().
 *
 * Override connection details by defining the DB_* constants (and, for
 * uploaded media, SHARED_STORAGE_DIR) before requiring this file — e.g. in
 * each app's own config/config.php — otherwise the defaults below are used.
 */

declare(strict_types=1);

if (!defined('DB_HOST')) define('DB_HOST', getenv('NANNYAPP_DB_HOST') ?: '127.0.0.1');
if (!defined('DB_PORT')) define('DB_PORT', getenv('NANNYAPP_DB_PORT') ?: '3306');
if (!defined('DB_NAME')) define('DB_NAME', getenv('NANNYAPP_DB_NAME') ?: 'nanny_app');
if (!defined('DB_USER')) define('DB_USER', getenv('NANNYAPP_DB_USER') ?: 'root');
if (!defined('DB_PASS')) define('DB_PASS', getenv('NANNYAPP_DB_PASS') ?: '');

// Where uploaded files (profile photos, verification docs, etc.) live —
// outside any single app's webroot so web + app share the exact same files.
if (!defined('SHARED_STORAGE_DIR')) define('SHARED_STORAGE_DIR', __DIR__ . '/../storage');

require_once __DIR__ . '/storage.php';

function db(): PDO
{
    static $pdo = null;

    if ($pdo === null) {
        $dsn = sprintf(
            'mysql:host=%s;port=%s;dbname=%s;charset=utf8mb4',
            DB_HOST,
            DB_PORT,
            DB_NAME
        );

        try {
            $pdo = new PDO($dsn, DB_USER, DB_PASS, [
                PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
                PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
                PDO::ATTR_EMULATE_PREPARES   => false,
            ]);
        } catch (PDOException $e) {
            http_response_code(500);
            error_log('NannyApp database connection failed: ' . $e->getMessage());
            exit('Database connection failed. Check the server configuration and database migration status.');
        }
    }

    return $pdo;
}
