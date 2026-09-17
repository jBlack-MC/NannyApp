<?php
/** POST /api/admin/users_suspend.php { userId } */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();
require_api_role($me, 'admin');

$userId = (int) (json_body()['userId'] ?? 0);
if ($userId === (int) $me['id']) json_error('You cannot suspend your own account.');

db()->prepare("UPDATE users SET status = 'suspended' WHERE id = :id")->execute(['id' => $userId]);
db()->prepare('DELETE FROM api_tokens WHERE user_id = :id')->execute(['id' => $userId]); // force logout everywhere
notify($userId, 'Account suspended', 'Your account has been suspended. Contact support for more information.');

json_response(true, null);
