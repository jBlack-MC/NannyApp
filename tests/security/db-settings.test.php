<?php
// Database selection: a connection URL wins as a whole, so leftover fields never mix projects.
declare(strict_types=1);

require __DIR__ . '/../../NannyApp.Shared/config/db_settings.php';

function check(bool $ok, string $what): void
{
    if (!$ok) { fwrite(STDERR, "FAILED: $what\n"); exit(1); }
}

$vars = ['NANNYAPP_DB_URL', 'POSTGRES_URL_NON_POOLING', 'NANNYAPP_DB_DRIVER', 'NANNYAPP_DB_HOST', 'NANNYAPP_DB_PORT', 'NANNYAPP_DB_NAME',
    'NANNYAPP_DB_USER', 'NANNYAPP_DB_PASS', 'NANNYAPP_DB_SSLMODE', 'POSTGRES_HOST', 'POSTGRES_PORT', 'POSTGRES_DATABASE', 'POSTGRES_USER', 'POSTGRES_PASSWORD'];
$with = function (array $set) use ($vars): array {
    foreach ($vars as $v) putenv($v);
    foreach ($set as $k => $v) putenv("$k=$v");
    return nannyapp_db_settings();
};

// Old hand-entered fields for another project plus the Supabase integration: the integration wins entirely.
$s = $with([
    'NANNYAPP_DB_HOST' => 'old-project.pooler.supabase.com', 'NANNYAPP_DB_USER' => 'postgres.oldproject', 'NANNYAPP_DB_PASS' => 'old',
    'POSTGRES_URL_NON_POOLING' => 'postgres://postgres.newproject:p%40ss@aws-0-us-east-1.pooler.supabase.com:5432/postgres?sslmode=require',
]);
check($s['source'] === 'POSTGRES_URL_NON_POOLING' && $s['driver'] === 'pgsql', 'integration URL chosen');
check($s['host'] === 'aws-0-us-east-1.pooler.supabase.com' && $s['user'] === 'postgres.newproject' && $s['pass'] === 'p@ss', 'no mixing, password decoded');
check($s['port'] === '5432' && $s['name'] === 'postgres' && $s['sslmode'] === 'require', 'port, database and sslmode from URL');

// An explicit NANNYAPP_DB_URL overrides the integration.
$s = $with(['NANNYAPP_DB_URL' => 'postgresql://u:p@explicit.example:6000/app?sslmode=disable', 'POSTGRES_URL_NON_POOLING' => 'postgres://x:y@integration.example/postgres']);
check($s['host'] === 'explicit.example' && $s['port'] === '6000' && $s['name'] === 'app' && $s['sslmode'] === 'disable', 'explicit URL first');

// No URL: separate fields, NANNYAPP_* first, then POSTGRES_*.
$s = $with(['POSTGRES_HOST' => 'pg.example', 'POSTGRES_USER' => 'pguser', 'NANNYAPP_DB_NAME' => 'nanny']);
check($s['source'] === 'fields' && $s['driver'] === 'pgsql' && $s['host'] === 'pg.example' && $s['user'] === 'pguser' && $s['name'] === 'nanny', 'fields');

// Local XAMPP defaults.
$s = $with([]);
check($s['driver'] === 'mysql' && $s['host'] === '127.0.0.1' && $s['port'] === '3306' && $s['name'] === 'nanny_app' && $s['user'] === 'root', 'local defaults');

// A malformed URL is ignored rather than half-used.
$s = $with(['POSTGRES_URL_NON_POOLING' => 'not a url', 'NANNYAPP_DB_HOST' => 'fallback.example']);
check($s['source'] === 'fields' && $s['host'] === 'fallback.example', 'malformed URL ignored');

echo "Database settings checks passed: URL wins whole, explicit override, fields fallback, local defaults.\n";
