<?php
/** POST /api/auth/reset.php { token, newPassword } */
require_once __DIR__ . '/../_bootstrap.php';

$b = json_body();
$token = (string) ($b['token'] ?? '');
$newPassword = (string) ($b['newPassword'] ?? '');

if ($token === '' || strlen($newPassword) < 8) {
    json_error('Please provide a valid token and a password of at least 8 characters.');
}

$stmt = db()->prepare('SELECT * FROM password_resets WHERE token = :token AND used = 0 AND expires_at > NOW()');
$stmt->execute(['token' => $token]);
$reset = $stmt->fetch();

if (!$reset) {
    json_error('This reset link is invalid or has expired. Please request a new one.', 400);
}

db()->prepare('UPDATE users SET password_hash = :hash WHERE id = :uid')
    ->execute(['hash' => password_hash($newPassword, PASSWORD_DEFAULT), 'uid' => $reset['user_id']]);
db()->prepare('UPDATE password_resets SET used = 1 WHERE id = :id')->execute(['id' => $reset['id']]);
// Revoke all existing sessions/tokens for safety after a password reset.
db()->prepare('DELETE FROM api_tokens WHERE user_id = :uid')->execute(['uid' => $reset['user_id']]);

json_response(true, null, 'Your password has been reset. Please log in.');
