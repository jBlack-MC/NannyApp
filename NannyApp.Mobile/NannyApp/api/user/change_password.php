<?php
/** POST /api/user/change_password.php { current_password, new_password } */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();

$b = json_body();
$current = (string) ($b['current_password'] ?? '');
$new = (string) ($b['new_password'] ?? '');

if (!password_verify($current, $me['password_hash'])) {
    json_error('Your current password is incorrect.', 400);
}
if (strlen($new) < 8) {
    json_error('New password must be at least 8 characters.');
}

db()->prepare('UPDATE users SET password_hash = :hash WHERE id = :id')
    ->execute(['hash' => password_hash($new, PASSWORD_DEFAULT), 'id' => $me['id']]);

json_response(true, null, 'Password updated.');
