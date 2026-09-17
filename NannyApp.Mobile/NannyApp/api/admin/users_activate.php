<?php
/** POST /api/admin/users_activate.php { userId } */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();
require_api_role($me, 'admin');

$userId = (int) (json_body()['userId'] ?? 0);
db()->prepare("UPDATE users SET status = 'active' WHERE id = :id")->execute(['id' => $userId]);
notify($userId, 'Account reactivated', 'Your account has been reactivated. Welcome back!');

json_response(true, null);
