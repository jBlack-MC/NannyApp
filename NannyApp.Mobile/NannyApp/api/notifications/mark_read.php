<?php
/** POST /api/notifications/mark_read.php { id } */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();

$id = (int) (json_body()['id'] ?? 0);
db()->prepare('UPDATE notifications SET is_read = 1 WHERE id = :id AND user_id = :uid')
    ->execute(['id' => $id, 'uid' => $me['id']]);

json_response(true, null);
