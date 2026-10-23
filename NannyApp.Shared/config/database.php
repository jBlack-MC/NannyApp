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

// The website and the native-app API must pick the same database: see db_settings.php.
require_once __DIR__ . '/db_settings.php';
nannyapp_define_db_constants();
if (!in_array(DB_DRIVER, ['mysql', 'pgsql'], true)) {
    throw new RuntimeException('NANNYAPP_DB_DRIVER must be mysql or pgsql.');
}

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

    // These legacy callers need only the first column (the column name).
    if (preg_match('/^\s*SHOW COLUMNS FROM ([a-z_][a-z0-9_]*)\s*;?\s*$/i', $sql, $columns)) {
        return "SELECT column_name FROM information_schema.columns WHERE table_schema=current_schema() AND table_name='" . $columns[1] . "' ORDER BY ordinal_position";
    }

    $sql = preg_replace('/\bNOW\(\)/i', 'CURRENT_TIMESTAMP', $sql) ?? $sql;
    $sql = preg_replace('/\bCURDATE\(\)/i', 'CURRENT_DATE', $sql) ?? $sql;
    // Dashboard charts use only these two MySQL date formats. Keep the
    // conversion explicit so unsupported format strings are not misinterpreted.
    $sql = preg_replace_callback(
        "/\\bDATE_FORMAT\\(\\s*([a-z_][a-z0-9_.]*)\\s*,\\s*'(%Y-%m|%b %Y|%b)'\\s*\\)/i",
        static fn(array $m): string => "TO_CHAR({$m[1]}, '" . ['%Y-%m' => 'YYYY-MM', '%b' => 'Mon', '%b %Y' => 'Mon YYYY'][$m[2]] . "')",
        $sql
    ) ?? $sql;
    $sql = preg_replace_callback(
        '/\\b(MONTH|YEAR)\\(\\s*([a-z_][a-z0-9_.]*)\\s*\\)/i',
        static fn(array $m): string => 'EXTRACT(' . strtoupper($m[1]) . " FROM {$m[2]})",
        $sql
    ) ?? $sql;
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

function db(bool $optional = false): ?PDO
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
            if ($optional) {
                log_exception($e, 'optional_database_connection_failed');
                return null;
            }
            // CLI workers must fail with a nonzero exit, not exit(string)'s success code.
            if (PHP_SAPI === 'cli') throw $e;
            http_response_code(500);
            log_exception($e, 'database_connection_failed');
            exit('Database connection failed. Check the server configuration and database migration status. Reference: ' . request_ref());
        }
    }

    return $pdo;
}
