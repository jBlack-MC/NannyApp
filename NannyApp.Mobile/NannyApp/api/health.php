<?php
/**
 * GET /api/health.php — for an external uptime monitor.
 *
 * 200 {"status":"ok"} when the database answers, the delivery queues are
 * moving and the upload disk has space; 503 otherwise. Public, so it reports
 * only which check failed, never counts or details — those are on the admin
 * action centre and in the server log.
 */
require_once __DIR__ . '/_bootstrap.php';
require_once __DIR__ . '/../../../NannyApp.Shared/config/health.php';

function health_reply(int $httpCode, string $status, array $checks): never
{
    http_response_code($httpCode);
    echo json_encode(['status' => $status, 'checks' => $checks]);
    exit;
}

// db() exits with a plain-text 500 on failure; connect directly so monitors always get JSON.
try {
    $pdo = db();
} catch (PDOException $e) {
    log_exception($e, 'health_database_down');
    health_reply(503, 'down', ['database' => 'failed']);
}

$health = system_health($pdo);
$checks = array_map(static fn (array $check): string => $check['ok'] ? 'ok' : 'failed', $health['checks']);
if ($health['status'] !== 'ok') {
    log_event('warning', 'health_degraded', ['failed' => array_keys($checks, 'failed', true)]);
}
health_reply($health['status'] === 'ok' ? 200 : 503, $health['status'], $checks);
