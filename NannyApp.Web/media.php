<?php
declare(strict_types=1);
require_once __DIR__ . '/config/config.php';
$requested = (string) ($_GET['f'] ?? '');
header('Cache-Control: private, no-store');
header('X-Content-Type-Options: nosniff');
header('Referrer-Policy: no-referrer');
if (!media_access_allowed($requested, current_user())) {
    http_response_code(403);
    exit('Access denied.');
}
if (STORAGE_DRIVER === 's3') {
    header('Location: ' . s3_presigned_url($requested, 60));
    exit;
}
if (STORAGE_DRIVER === 'database') {
    $stmt = db()->prepare('SELECT mime, data_base64 FROM stored_media WHERE path = ?');
    $stmt->execute([$requested]);
    $media = $stmt->fetch();
    $body = $media ? base64_decode($media['data_base64'], true) : false;
    if ($body === false) { http_response_code(404); exit('Not found.'); }
    header('Content-Type: ' . $media['mime']);
    header('Content-Length: ' . strlen($body));
    echo $body;
    exit;
}
$root = realpath(SHARED_STORAGE_DIR);
$full = $root === false ? false : realpath($root . '/' . $requested);
if ($root === false || $full === false || !str_starts_with($full, $root . DIRECTORY_SEPARATOR) || !is_file($full)) {
    http_response_code(404);
    exit('Not found.');
}
header('Content-Type: ' . storage_mime_for($full));
header('Content-Length: ' . filesize($full));
readfile($full);
