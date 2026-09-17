<?php
/** POST /api/notifications/mark_all_read.php */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();

db()->prepare('UPDATE notifications SET is_read = 1 WHERE user_id = :uid')->execute(['uid' => $me['id']]);

json_response(true, null);
