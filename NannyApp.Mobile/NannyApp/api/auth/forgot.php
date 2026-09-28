<?php
require_once __DIR__ . '/../_bootstrap.php';
if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') json_error('Method not allowed.', 405);
$email = strtolower(trim((string) (json_body()['email'] ?? '')));
if (!filter_var($email, FILTER_VALIDATE_EMAIL)) json_error('Please enter a valid email.');
if (auth_rate_limited('forgot_password', $email, 3)) json_error('Too many requests. Try again later.', 429);
$stmt = db()->prepare('SELECT id, full_name, email_verified FROM users WHERE email = ?');
$stmt->execute([$email]);
$user = $stmt->fetch();
if ($user) send_password_reset_email((int) $user['id'], $email, $user['full_name']);
json_response(true, (object) [], 'If eligible, instructions will arrive by email. If they do not arrive, please retry later.');
