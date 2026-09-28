<?php
/** POST /api/auth/reset.php { token, newPassword } */
require_once __DIR__ . '/../_bootstrap.php';

if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') json_error('Method not allowed.', 405);
$b = json_body();
$token = (string) ($b['token'] ?? '');
$newPassword = (string) ($b['newPassword'] ?? '');

if ($token === '' || strlen($newPassword) < 8) {
    json_error('Please provide a valid token and a password of at least 8 characters.');
}

if (auth_rate_limited('reset', $token, 5)) json_error('Too many requests. Try again later.', 429);
if (!consume_password_reset($token, $newPassword)) json_error('This reset link is invalid or has expired.', 400);
json_response(true, (object) [], 'Your password has been reset. Please log in.');
