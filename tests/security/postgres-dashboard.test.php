<?php
declare(strict_types=1);

define('DB_DRIVER', $argv[1] ?? 'pgsql');
require __DIR__ . '/../../NannyApp.Shared/config/database.php';

$cases = [
    "SELECT DATE_FORMAT(pay.created_at,'%Y-%m'), DATE_FORMAT(date_time,'%b')" => "SELECT TO_CHAR(pay.created_at, 'YYYY-MM'), TO_CHAR(date_time, 'Mon')",
    'SELECT MONTH(pay.created_at)=MONTH(NOW()), YEAR(pay.created_at)=YEAR(NOW())' => 'SELECT EXTRACT(MONTH FROM pay.created_at)=EXTRACT(MONTH FROM CURRENT_TIMESTAMP), EXTRACT(YEAR FROM pay.created_at)=EXTRACT(YEAR FROM CURRENT_TIMESTAMP)',
    "SELECT SUM(CASE WHEN status='completed' THEN 1 ELSE 0 END), COALESCE(SUM(amount),0)" => "SELECT SUM(CASE WHEN status='completed' THEN 1 ELSE 0 END), COALESCE(SUM(amount),0)",
];
foreach ($cases as $input => $expected) {
    if (nannyapp_pg_sql($input) !== (DB_DRIVER === 'pgsql' ? $expected : $input)) {
        throw new RuntimeException('Dashboard SQL translation failed: ' . $input);
    }
}

// Export the actual dashboard queries for read-only EXPLAIN checks on PostgreSQL.
$queries = [];
foreach (['parent', 'nanny', 'admin'] as $role) {
    $source = file_get_contents(__DIR__ . '/../../NannyApp.Web/' . $role . '/dashboard.php');
    preg_match_all('/"(SELECT\b[^"]*)"/s', $source, $matches);
    foreach ($matches[1] as $query) {
        if (preg_match('/SUM\(status\s*=|SUM\(date_time\s*>|IFNULL\(/i', $query)) {
            throw new RuntimeException('MySQL-only dashboard aggregate remains: ' . $role);
        }
        $queries[] = str_replace('?', '1', nannyapp_pg_sql($query));
    }
}
if (in_array('--export', $argv, true)) {
    echo json_encode($queries);
} else {
    echo 'PASS dashboard SQL: ' . DB_DRIVER . ', ' . count($queries) . " queries inspected.\n";
}
