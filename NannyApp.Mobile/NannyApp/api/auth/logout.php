<?php
/** POST /api/auth/logout.php — revokes the bearer token used for this request. */
require_once __DIR__ . '/../_bootstrap.php';

if (preg_match('/Bearer\s+(\S+)/i', request_authorization(), $m)) {
    $hash = hash('sha256', $m[1]);
    $stmt = db()->prepare('SELECT user_id FROM api_tokens WHERE token_hash = :hash');
    $stmt->execute(['hash' => $hash]);
    $userId = $stmt->fetchColumn();
    if ($userId !== false) revoke_all_user_sessions((int) $userId);
}
json_response(true, (object) []);
