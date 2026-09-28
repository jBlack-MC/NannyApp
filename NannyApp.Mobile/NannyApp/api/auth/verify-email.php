<?php
/** POST /api/auth/verify-email.php { token } */
require_once __DIR__ . '/../_bootstrap.php';

if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') json_error('Method not allowed.', 405);
$token = (string) (json_body()['token'] ?? '');
if ($token === '') {
    json_error('Missing verification token.');
}

if (auth_rate_limited('verify', $token, 5)) json_error('Too many requests. Try again later.', 429);
if (!consume_verification_token($token)) json_error('This verification link is invalid or expired.', 400);
json_response(true, (object) [], 'Your email has been verified. You can now log in.');
