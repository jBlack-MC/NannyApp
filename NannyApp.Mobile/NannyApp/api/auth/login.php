<?php
/** POST /api/auth/login.php  { email, password, rememberMe } -> { token, user } */
require_once __DIR__ . '/../_bootstrap.php';

if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') {
    json_error('Method not allowed.', 405);
}

$body = json_body();
$email = trim((string) ($body['email'] ?? ''));
$password = (string) ($body['password'] ?? '');

if ($email === '' || $password === '') {
    json_error('Please enter your email and password.');
}

$ip = $_SERVER['REMOTE_ADDR'] ?? '0.0.0.0';
if (is_rate_limited('api_login_' . $ip, 5, 900)) {
    json_error('Too many login attempts. Please wait 15 minutes and try again.', 429);
}

$stmt = db()->prepare('SELECT * FROM users WHERE email = :email');
$stmt->execute(['email' => $email]);
$user = $stmt->fetch();

if (!$user || !password_verify($password, $user['password_hash'])) {
    increment_rate_limit('api_login_' . $ip);
    json_error('Incorrect email or password. Please try again.', 401);
}

if ((int) $user['email_verified'] === 0) {
    json_error('Please verify your email before logging in. Check your inbox for the verification link.', 403);
}

if ($user['status'] === 'suspended') {
    json_error('Your account has been suspended. Contact support for help.', 403);
}

// Administration is intentionally web-only. Do this before issuing an API
// token so an admin account cannot be used by the packaged mobile client.
if ($user['role'] === 'admin') {
    json_error('Administrator accounts are available through the web portal only.', 403);
}

$token = issue_api_token((int) $user['id'], $_SERVER['HTTP_USER_AGENT'] ?? null);

json_response(true, [
    'token' => $token,
    'user' => [
        'id' => (int) $user['id'],
        'fullName' => $user['full_name'],
        'email' => $user['email'],
        'phone' => $user['phone'],
        'role' => $user['role'],
        'status' => $user['status'],
        'emailVerified' => (bool) $user['email_verified'],
        'profileImage' => media_url($user['profile_image']),
        'dateOfBirth' => $user['date_of_birth'],
        'address' => $user['address'],
        'gender' => $user['gender'],
        'createdAt' => $user['created_at'],
    ],
]);
