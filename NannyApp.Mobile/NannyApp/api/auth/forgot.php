<?php
/** POST /api/auth/forgot.php { email } -> always 200, never reveals if the email exists. */
require_once __DIR__ . '/../_bootstrap.php';

$email = trim((string) (json_body()['email'] ?? ''));
if ($email === '') {
    json_error('Please enter your email.');
}

$stmt = db()->prepare('SELECT id FROM users WHERE email = :email');
$stmt->execute(['email' => $email]);
$user = $stmt->fetch();

if ($user) {
    $token = bin2hex(random_bytes(32));
    $expires = (new DateTime())->modify('+1 hour')->format('Y-m-d H:i:s');
    db()->prepare('INSERT INTO password_resets (user_id, token, expires_at) VALUES (:uid, :token, :exp)')
        ->execute(['uid' => $user['id'], 'token' => $token, 'exp' => $expires]);
    // TODO (deployment): email the reset link via includes/email.php, e.g.
    // url("/reset-password.php?token=$token")
}

json_response(true, null, 'If an account exists for that email, reset instructions have been sent.');
