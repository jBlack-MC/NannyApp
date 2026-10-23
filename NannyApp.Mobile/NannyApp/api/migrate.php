<?php
/** One-time protected migration endpoint for serverless deployments. */
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
require_once __DIR__ . '/../../../NannyApp.Shared/config/migrations.php';
header('Content-Type: application/json; charset=utf-8');

try {
    migrate_application(db());
    $nativeCount = (int) db()->query("SELECT COUNT(*) FROM schema_migrations WHERE name LIKE 'postgres/%'")->fetchColumn();
    echo json_encode(['ok' => true, 'message' => 'Schema is up to date.',
        'schemaVersion' => 'migrate_v14_contact_messages.sql', 'nativeMigrationCount' => $nativeCount]);
} catch (Throwable $e) {
    log_exception($e, 'serverless_migration_failed');
    http_response_code(500);
    echo json_encode(['ok' => false, 'message' => 'Migration failed. Check Vercel logs.']);
}
