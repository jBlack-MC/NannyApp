<?php
/** POST /api/auth/logout.php — revokes the bearer token used for this request. */
require_once __DIR__ . '/../_bootstrap.php';

$header = $_SERVER['HTTP_AUTHORIZATION'] ?? '';
if (preg_match('/Bearer\s+(\S+)/i', $header, $m)) {
    $hash = hash('sha256', $m[1]);
    db()->prepare('DELETE FROM api_tokens WHERE token_hash = :hash')->execute(['hash' => $hash]);
}
json_response(true, null);
