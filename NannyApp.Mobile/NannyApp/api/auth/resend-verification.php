<?php
/** POST /api/auth/resend-verification.php { email } */
require_once __DIR__ . '/../_bootstrap.php';

$email = trim((string) (json_body()['email'] ?? ''));
$stmt = db()->prepare('SELECT id, email_verified FROM users WHERE email = :email');
$stmt->execute(['email' => $email]);
$user = $stmt->fetch();

if ($user && (int) $user['email_verified'] === 0) {
    $token = bin2hex(random_bytes(32));
    db()->prepare('UPDATE users SET verification_token = :token, verification_sent_at = NOW() WHERE id = :id')
        ->execute(['token' => $token, 'id' => $user['id']]);
    // TODO (deployment): send via includes/email.php
}

json_response(true, null, 'If that email exists and is unverified, a new verification link has been sent.');
