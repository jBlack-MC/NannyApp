<?php
if (PHP_SAPI !== 'cli') { http_response_code(404); exit; }
require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../config/migrations.php';
try {
    foreach (['NANNYAPP_DB_NAME','NANNYAPP_DB_USER'] as $key) {
        if (!getenv($key)) throw new RuntimeException('Set ' . $key . ' explicitly.');
    }
    migrate_application(db());
    echo "Schema is up to date. No demo accounts were created.\n";
} catch (Throwable $e) {
    fwrite(STDERR, "Migration failed: " . $e->getMessage() . "\n");
    exit(1);
}
