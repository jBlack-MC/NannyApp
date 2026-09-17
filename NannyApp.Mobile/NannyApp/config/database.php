<?php
/**
 * DB connection (singleton) shared with the web app.
 *
 * If NannyApp.Shared is checked out alongside this project (monorepo
 * layout), reuse its db() + SHARED_STORAGE_DIR so the Android app reads/
 * writes the exact same MySQL database and uploaded files as the web app.
 * Otherwise (this project deployed standalone) fall back to a local-only
 * connection using the DB_* constants from config/db_credentials.php.
 */

declare(strict_types=1);

$sharedConfig = __DIR__ . '/../../../NannyApp.Shared/config/database.php';

if (is_file($sharedConfig)) {
    require_once $sharedConfig;
} else {
    if (!defined('SHARED_STORAGE_DIR')) define('SHARED_STORAGE_DIR', __DIR__ . '/../assets');

    // Standalone mode only supports local disk storage (no NannyApp.Shared
    // means no shared config for S3/etc.) — sharded by month so no single
    // folder grows unbounded.
    if (!function_exists('storage_new_path')) {
        function storage_new_path(string $subdir, string $ext): string
        {
            return trim($subdir, '/\\') . '/' . date('Y/m') . '/' . bin2hex(random_bytes(8)) . '.' . $ext;
        }

        function storage_store_upload(string $uploadedTmpPath, string $relativePath): bool
        {
            $full = rtrim(SHARED_STORAGE_DIR, '/\\') . '/' . $relativePath;
            $dir  = dirname($full);
            if (!is_dir($dir) && !@mkdir($dir, 0775, true) && !is_dir($dir)) {
                return false;
            }
            return move_uploaded_file($uploadedTmpPath, $full);
        }

        function storage_delete(string $relativePath): void
        {
            @unlink(rtrim(SHARED_STORAGE_DIR, '/\\') . '/' . $relativePath);
        }

        function storage_url(string $relativePath, callable $localMediaUrl): string
        {
            return $localMediaUrl($relativePath);
        }
    }

    function db(): PDO
    {
        static $pdo = null;

        if ($pdo === null) {
            $dsn = sprintf(
                'mysql:host=%s;port=%s;dbname=%s;charset=utf8mb4',
                DB_HOST,
                DB_PORT,
                DB_NAME
            );

            try {
                $pdo = new PDO($dsn, DB_USER, DB_PASS, [
                    PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
                    PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
                    PDO::ATTR_EMULATE_PREPARES   => false,
                ]);
            } catch (PDOException $e) {
                http_response_code(500);
                exit('Database connection failed. Have you imported database/schema.sql and is MySQL running? '
                    . '(' . htmlspecialchars($e->getMessage()) . ')');
            }
        }

        return $pdo;
    }
}
