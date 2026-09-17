<?php
/** POST /api/auth/verify-email.php { token } */
require_once __DIR__ . '/../_bootstrap.php';

$token = (string) (json_body()['token'] ?? '');
if ($token === '') {
    json_error('Missing verification token.');
}

$stmt = db()->prepare('SELECT id FROM users WHERE verification_token = :token');
$stmt->execute(['token' => $token]);
$user = $stmt->fetch();

if (!$user) {
    json_error('This verification link is invalid or has already been used.', 400);
}

db()->prepare('UPDATE users SET email_verified = 1, verification_token = NULL WHERE id = :id')
    ->execute(['id' => $user['id']]);

json_response(true, null, 'Your email has been verified. You can now log in.');
