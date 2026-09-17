<?php
/**
 * Streams uploaded media (profile photos, verification docs, etc.) from
 * NannyApp.Shared/storage/. Kept outside this project's webroot so the
 * web app and packaged app share one copy of every file instead of each
 * project storing its own.
 */

declare(strict_types=1);

require_once __DIR__ . '/config/config.php';

$requested = (string) ($_GET['f'] ?? '');
if ($requested === '') {
    http_response_code(400);
    exit('Missing file.');
}

// Reject path traversal / absolute paths outright before touching the filesystem.
if (str_contains($requested, "\0") || str_contains($requested, '..') || preg_match('#^[/\\\\]|^[a-zA-Z]:#', $requested)) {
    http_response_code(400);
    exit('Invalid path.');
}

$root = realpath(SHARED_STORAGE_DIR);
$full = realpath($root . '/' . $requested);

// Confirm the resolved file is actually inside the storage root.
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
