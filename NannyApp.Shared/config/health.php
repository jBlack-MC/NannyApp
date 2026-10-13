<?php
/**
 * Read-only health checks shared by GET /api/health.php (for an uptime
 * monitor) and the admin action centre.
 *
 * There is no worker heartbeat table, so worker health is inferred from the
 * queues: a job that has been ready for longer than HEALTH_QUEUE_STALL_MINUTES
 * means the worker is stopped or failing. An empty queue proves nothing either
 * way, but nothing is waiting on the worker in that case.
 */

declare(strict_types=1);

const HEALTH_QUEUE_STALL_MINUTES = 10;

/** @return array{status:string,checks:array<string,array{ok:bool,detail:string}>} */
function system_health(PDO $pdo): array
{
    $checks = ['database' => ['ok' => true, 'detail' => 'Connected']];

    try {
        $age = $pdo->query("SELECT
            (SELECT TIMESTAMPDIFF(MINUTE, MIN(created_at), NOW()) FROM notification_outbox WHERE delivered_at IS NULL) AS notifications,
            (SELECT TIMESTAMPDIFF(MINUTE, MIN(available_at), NOW()) FROM email_outbox
              WHERE status IN ('pending','sending') AND expires_at > NOW()) AS emails")->fetch(PDO::FETCH_ASSOC);
        foreach (['notifications' => 'notification', 'emails' => 'account email'] as $key => $label) {
            $minutes = $age[$key] === null ? null : (int) $age[$key];
            $stalled = $minutes !== null && $minutes >= HEALTH_QUEUE_STALL_MINUTES;
            $checks[$key] = ['ok' => !$stalled, 'detail' => match (true) {
                $stalled => "Oldest $label has waited $minutes minutes; check the worker",
                $minutes === null => 'Nothing waiting',
                default => 'Being processed',
            }];
        }
    } catch (PDOException $e) {
        // Missing queue tables mean migrations have not been applied to this database.
        log_exception($e, 'health_queue_check_failed');
        $checks['migrations'] = ['ok' => false, 'detail' => 'Queue tables could not be read; run the migration command'];
    }

    if ((getenv('NANNYAPP_STORAGE_DRIVER') ?: 'local') === 'local') {
        $reserve = (int) (getenv('NANNYAPP_STORAGE_RESERVE_BYTES') ?: '20971520');
        $free = is_dir(SHARED_STORAGE_DIR) ? @disk_free_space(SHARED_STORAGE_DIR) : false;
        $checks['storage'] = $free === false
            ? ['ok' => false, 'detail' => 'Upload directory is missing or unreadable']
            : ['ok' => $free > $reserve, 'detail' => round($free / 1048576) . ' MiB free on the upload disk'];
    }

    $healthy = !in_array(false, array_column($checks, 'ok'), true);
    return ['status' => $healthy ? 'ok' : 'degraded', 'checks' => $checks];
}
