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

if (!defined('DB_DRIVER')) define('DB_DRIVER', strtolower((string)(getenv('NANNYAPP_DB_DRIVER') ?: 'mysql')));
if (!in_array(DB_DRIVER, ['mysql', 'pgsql'], true)) {
    throw new RuntimeException('NANNYAPP_DB_DRIVER must be mysql or pgsql.');
}
if (!defined('DB_HOST')) define('DB_HOST', getenv('NANNYAPP_DB_HOST') ?: '127.0.0.1');
if (!defined('DB_PORT')) define('DB_PORT', getenv('NANNYAPP_DB_PORT') ?: '3306');
if (!defined('DB_NAME')) define('DB_NAME', getenv('NANNYAPP_DB_NAME') ?: 'nanny_app');
if (!defined('DB_USER')) define('DB_USER', getenv('NANNYAPP_DB_USER') ?: 'root');
if (!defined('DB_PASS')) define('DB_PASS', getenv('NANNYAPP_DB_PASS') ?: '');
if (!defined('DB_SSLMODE')) define('DB_SSLMODE', getenv('NANNYAPP_DB_SSLMODE') ?: 'require');

// Where uploaded files (profile photos, verification docs, etc.) live —
// outside any single app's webroot so web + app share the exact same files.
if (!defined('SHARED_STORAGE_DIR')) define('SHARED_STORAGE_DIR', getenv('NANNYAPP_STORAGE_DIR') ?: __DIR__ . '/../storage');

require_once __DIR__ . '/logging.php';
require_once __DIR__ . '/storage.php';

/**
 * Translate the small set of MySQL compatibility expressions still used by
 * the legacy application when it is connected to Supabase PostgreSQL.
 * The MySQL path is left completely untouched.
 */
function nannyapp_pg_sql(string $sql): string
{
    if (DB_DRIVER !== 'pgsql') return $sql;

    $sql = preg_replace('/\bNOW\(\)/i', 'CURRENT_TIMESTAMP', $sql) ?? $sql;
    $sql = preg_replace('/\bCURDATE\(\)/i', 'CURRENT_DATE', $sql) ?? $sql;
    $sql = preg_replace('/\bUNIX_TIMESTAMP\(\)/i', 'EXTRACT(EPOCH FROM CURRENT_TIMESTAMP)::bigint', $sql) ?? $sql;
    $sql = preg_replace_callback('/SHA2\(([^,]+),\s*256\)/i', static fn(array $m): string => "encode(digest({$m[1]}, 'sha256'), 'hex')", $sql) ?? $sql;

    // MySQL's scalar IF(condition, true, false) is equivalent to CASE in
    // PostgreSQL. The application uses this form only with simple operands.
    $sql = preg_replace_callback('/\bIF\(\s*([^,()]+)\s*,\s*([^,()]+)\s*,\s*([^()]+)\)/i', static fn(array $m): string => "(CASE WHEN {$m[1]} THEN {$m[2]} ELSE {$m[3]} END)", $sql) ?? $sql;

    // PostgreSQL does not accept MySQL's DATE_ADD/DATE_SUB INTERVAL syntax.
    $sql = preg_replace_callback(
        '/DATE_SUB\(\s*CURRENT_TIMESTAMP\s*,\s*INTERVAL\s+([0-9]+)\s+(SECOND|MINUTE|HOUR|DAY|WEEK|MONTH|YEAR)\s*\)/i',
        static fn(array $m): string => "(CURRENT_TIMESTAMP - INTERVAL '{$m[1]} {$m[2]}')",
        $sql
    ) ?? $sql;
    $sql = preg_replace_callback(
        '/DATE_ADD\(\s*CURRENT_TIMESTAMP\s*,\s*INTERVAL\s+([0-9]+)\s+(SECOND|MINUTE|HOUR|DAY|WEEK|MONTH|YEAR)\s*\)/i',
        static fn(array $m): string => "(CURRENT_TIMESTAMP + INTERVAL '{$m[1]} {$m[2]}')",
        $sql
    ) ?? $sql;
    $sql = preg_replace_callback(
        '/DATE_SUB\(\s*CURRENT_TIMESTAMP\s*,\s*INTERVAL\s+\?\s+(SECOND|MINUTE|HOUR|DAY|WEEK|MONTH|YEAR)\s*\)/i',
        static fn(array $m): string => "(CURRENT_TIMESTAMP - (? * INTERVAL '1 {$m[1]}'))",
        $sql
    ) ?? $sql;
    $sql = preg_replace_callback(
        '/DATE_ADD\(\s*CURRENT_TIMESTAMP\s*,\s*INTERVAL\s+\?\s+(SECOND|MINUTE|HOUR|DAY|WEEK|MONTH|YEAR)\s*\)/i',
        static fn(array $m): string => "(CURRENT_TIMESTAMP + (? * INTERVAL '1 {$m[1]}'))",
        $sql
    ) ?? $sql;
    $sql = preg_replace_callback(
        '/CURRENT_TIMESTAMP\s*-\s*INTERVAL\s+\?\s+(SECOND|MINUTE|HOUR|DAY|WEEK|MONTH|YEAR)/i',
        static fn(array $m): string => "(CURRENT_TIMESTAMP - (? * INTERVAL '1 {$m[1]}'))",
        $sql
    ) ?? $sql;

    $sql = preg_replace_callback(
        '/TIMESTAMPDIFF\(\s*MINUTE\s*,\s*([^,]+)\s*,\s*CURRENT_TIMESTAMP\s*\)/i',
        static fn(array $m): string => "EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - {$m[1]})) / 60",
        $sql
    ) ?? $sql;

    // INSERT IGNORE means "discard a unique conflict" in the existing app.
    // PostgreSQL supports the same behavior with ON CONFLICT DO NOTHING.
    if (preg_match('/^\s*INSERT\s+IGNORE\s+/i', $sql)) {
        $sql = preg_replace('/^(\s*)INSERT\s+IGNORE\s+/i', '$1INSERT ', $sql) ?? $sql;
        if (!preg_match('/\bON\s+CONFLICT\b/i', $sql)) $sql = rtrim($sql, " \t\r\n;") . ' ON CONFLICT DO NOTHING';
    }
    return $sql;
}

final class NannyAppPostgresPDO extends PDO
{
    public function prepare(string $query, array $options = []): PDOStatement|false
    {
        return parent::prepare(nannyapp_pg_sql($query), $options);
    }

    public function exec(string $statement): int|false
    {
        return parent::exec(nannyapp_pg_sql($statement));
    }

    public function query(string $query, ?int $fetchMode = null, mixed ...$fetchModeArgs): PDOStatement|false
    {
        $query = nannyapp_pg_sql($query);
        return $fetchMode === null
            ? parent::query($query)
            : parent::query($query, $fetchMode, ...$fetchModeArgs);
    }
}

function db(): PDO
{
    static $pdo = null;

    if ($pdo === null) {
        if (DB_DRIVER === 'pgsql') {
            // Supabase's session pooler uses PostgreSQL over TLS. Use the
            // session pooler (port 5432), not transaction mode (6543), while
            // this application still relies on prepared statements.
            $dsn = sprintf(
                'pgsql:host=%s;port=%s;dbname=%s;sslmode=%s',
                DB_HOST,
                DB_PORT,
                DB_NAME,
                DB_SSLMODE
            );
        } else {
            $dsn = sprintf(
                'mysql:host=%s;port=%s;dbname=%s;charset=utf8mb4',
                DB_HOST,
                DB_PORT,
                DB_NAME
            );
        }

        try {
            $pdoClass = DB_DRIVER === 'pgsql' ? NannyAppPostgresPDO::class : PDO::class;
            $pdo = new $pdoClass($dsn, DB_USER, DB_PASS, [
                PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
                PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
                PDO::ATTR_EMULATE_PREPARES   => false,
            ]);
        } catch (PDOException $e) {
            // CLI workers must fail with a nonzero exit, not exit(string)'s success code.
            if (PHP_SAPI === 'cli') throw $e;
            http_response_code(500);
            log_exception($e, 'database_connection_failed');
            exit('Database connection failed. Check the server configuration and database migration status. Reference: ' . request_ref());
        }
    }

    return $pdo;
}
