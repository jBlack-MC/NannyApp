<?php
/**
 * POST /api/bots.php — creates/repairs only five named demo accounts.
 * Protected by the same token as api/migrate.php. Legacy bot deletion is disabled.
 * Body: {"action": "add", "demoPassword": required private password}.
 */
declare(strict_types=1);

if (($_SERVER['REQUEST_METHOD'] ?? 'GET') !== 'POST') {
    http_response_code(405);
    header('Allow: POST');
    exit;
}

$expected = (string) (getenv('NANNYAPP_MIGRATION_TOKEN') ?: '');
$provided = (string) ($_SERVER['HTTP_X_NANNY_MIGRATION_TOKEN'] ?? '');
if ($expected === '' || $provided === '' || !hash_equals($expected, $provided)) {
    http_response_code(404);
    exit;
}

require_once __DIR__ . '/../../../NannyApp.Shared/config/logging.php';
require_once __DIR__ . '/../../../NannyApp.Shared/config/database.php';
require_once __DIR__ . '/../../../NannyApp.Shared/config/demo_bots.php';
header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store');

$body = json_decode((string) file_get_contents('php://input'), true);
$action = is_array($body) ? (string) ($body['action'] ?? '') : '';
$demoPassword = is_array($body) ? (string) ($body['demoPassword'] ?? '') : '';

try {
    if ($action === 'add' && $demoPassword !== '') {
        $result = add_demo_bots(db());
        $result['demoAccountsCreated'] = ensure_demo_accounts(db(), $demoPassword);
        echo json_encode(['ok' => true] + $result);
    } else {
        http_response_code(400);
        echo json_encode(['ok' => false, 'message' => 'Use action "add" with a private demo password. Legacy bot deletion is disabled.']);
    }
} catch (InvalidArgumentException $e) {
    http_response_code(400);
    echo json_encode(['ok' => false, 'message' => $e->getMessage()]);
} catch (Throwable $e) {
    log_exception($e, 'demo_bots_failed');
    http_response_code(500);
    echo json_encode(['ok' => false, 'message' => 'Demo account setup failed. Check server logs.']);
}
