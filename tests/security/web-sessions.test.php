<?php
// Database sessions survive between "instances": a fresh handler and PHP session reads what another wrote.
// SQLite stands in for PostgreSQL (same ON CONFLICT upsert); no real database is used.
declare(strict_types=1);

define('DB_DRIVER', 'pgsql');
require __DIR__ . '/../../NannyApp.Shared/config/session.php';

function check(bool $ok, string $what): void
{
    if (!$ok) { fwrite(STDERR, "FAILED: $what\n"); exit(1); }
}

if (!in_array('sqlite', PDO::getAvailableDrivers(), true)) {
    echo "Web session checks skipped: this PHP has no pdo_sqlite.\n";
    exit(0);
}

$pdo = new PDO('sqlite::memory:', null, null, [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION]);
ini_set('session.use_cookies', '0');
ini_set('session.use_strict_mode', '1');

$handler = new NannyAppDatabaseSessionHandler($pdo, 1440);
check($handler->open('', 'PHPSESSID'), 'open creates the table');
check($handler->read('missing') === '' && !$handler->validateId('missing'), 'unknown IDs are empty and rejected');

$binary = "a\0b\xff";
check($handler->write('abc', 'csrf|s:4:"' . $binary . '";'), 'write');
check($handler->write('abc', 'csrf|s:5:"token";'), 'second write updates the row');
check((int) $pdo->query('SELECT COUNT(*) FROM web_sessions')->fetchColumn() === 1, 'one row per session');
check($handler->read('abc') === 'csrf|s:5:"token";' && $handler->validateId('abc'), 'read back');
check($handler->write('bin', $binary) && $handler->read('bin') === $binary, 'binary-safe');

$pdo->exec('UPDATE web_sessions SET updated_at = 1 WHERE id = \'abc\'');
check($handler->read('abc') === '' && !$handler->validateId('abc'), 'expired sessions are ignored');
check($handler->gc(1440) === 1, 'gc removes expired sessions');
check($handler->destroy('bin') && $handler->read('bin') === '', 'destroy');

// A full PHP session round trip, as two separate requests would do it.
session_set_save_handler($handler, true);
session_start();
$id = session_id();
$_SESSION['csrf'] = 'form-token';
session_write_close();
$_SESSION = [];
session_set_save_handler(new NannyAppDatabaseSessionHandler($pdo, 1440), true);
session_id($id);
session_start();
check(($_SESSION['csrf'] ?? null) === 'form-token', 'next request sees the CSRF token');
session_write_close();

// DB_DRIVER is pgsql in this test, as in production.
putenv('NANNYAPP_SESSION_DRIVER');
putenv('VERCEL');
check(nannyapp_database_sessions_wanted(), 'database sessions with PostgreSQL');
putenv('VERCEL=1');
check(nannyapp_database_sessions_wanted(), 'database sessions on Vercel');
putenv('NANNYAPP_SESSION_DRIVER=files');
check(!nannyapp_database_sessions_wanted(), 'files can be forced');
putenv('NANNYAPP_SESSION_DRIVER=database');
putenv('VERCEL');
check(nannyapp_database_sessions_wanted(), 'database can be forced');
$mysql = shell_exec(escapeshellarg(PHP_BINARY) . ' -r ' . escapeshellarg(
    "define('DB_DRIVER', 'mysql'); putenv('VERCEL'); putenv('NANNYAPP_SESSION_DRIVER'); require " . var_export(__DIR__ . '/../../NannyApp.Shared/config/session.php', true) . "; echo nannyapp_database_sessions_wanted() ? 'db' : 'files';"
));
check($mysql === 'files', 'local MySQL keeps file sessions');

echo "Web session checks passed: storage, expiry, binary data, PHP round trip and driver selection.\n";
