<?php
/**
 * POST /api/bots.php — adds or removes the demo bot accounts (NannyApp.Shared/config/demo_bots.php).
 * Protected by the same token as api/migrate.php and run from the "Demo bots" GitHub workflow.
 * Body: {"action": "add" | "remove", "demoPassword": optional password for the named demo accounts}.
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
    if ($action === 'remove') {
        echo json_encode(['ok' => true, 'removedUsers' => remove_demo_bots(db())]);
    } elseif ($action === 'add') {
        $result = add_demo_bots(db());
        if ($demoPassword !== '') $result['demoAccountsCreated'] = ensure_demo_accounts(db(), $demoPassword);
        echo json_encode(['ok' => true] + $result);
    } else {
        http_response_code(400);
        echo json_encode(['ok' => false, 'message' => 'action must be "add" or "remove".']);
    }
} catch (InvalidArgumentException $e) {
    http_response_code(400);
    echo json_encode(['ok' => false, 'message' => $e->getMessage()]);
} catch (Throwable $e) {
    log_exception($e, 'demo_bots_failed');
    http_response_code(500);
    echo json_encode(['ok' => false, 'message' => 'Demo bots failed. Check Vercel logs.']);
}
