<?php
/**
 * Streams uploaded media (avatars, portfolio docs, etc.) from SHARED_STORAGE_DIR
 * (NannyApp.Shared/storage/ when checked out, else this project's own assets/).
 * GET /api/media.php?f=<relative-path-returned-by-save_uploaded_image>
 */

declare(strict_types=1);

require_once __DIR__ . '/../config/db_credentials.php';
require_once __DIR__ . '/../config/database.php';

$requested = (string) ($_GET['f'] ?? '');
if ($requested === '') {
    http_response_code(400);
    exit('Missing file.');
}

if (str_contains($requested, "\0") || str_contains($requested, '..') || preg_match('#^[/\\\\]|^[a-zA-Z]:#', $requested)) {
    http_response_code(400);
    exit('Invalid path.');
}

$root = realpath(SHARED_STORAGE_DIR);
$full = realpath($root . '/' . $requested);

if ($root === false || $full === false || !str_starts_with($full, $root . DIRECTORY_SEPARATOR)) {
    http_response_code(404);
    exit('Not found.');
}

$ext = strtolower(pathinfo($full, PATHINFO_EXTENSION));
$mime = [
    'jpg'  => 'image/jpeg',
    'jpeg' => 'image/jpeg',
    'png'  => 'image/png',
    'webp' => 'image/webp',
    'gif'  => 'image/gif',
    'pdf'  => 'application/pdf',
][$ext] ?? null;

if ($mime === null) {
    http_response_code(403);
    exit('Unsupported file type.');
}

header('Content-Type: ' . $mime);
header('Content-Length: ' . (string) filesize($full));
header('Cache-Control: private, max-age=86400');
readfile($full);
