<?php
/** POST /api/user/change_password.php { current_password, new_password } */
require_once __DIR__ . '/../_bootstrap.php';
if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') json_error('Method not allowed.', 405);
$me = require_api_auth();
$b = json_body();
$current = $b['current_password'] ?? null;
$new = $b['new_password'] ?? null;
if (!is_string($current) || !is_string($new) || strlen($current) > 4096 || str_contains($current, "\0")
    || strlen($new) < 8 || strlen($new) > 72 || str_contains($new, "\0")) {
    json_error('Provide your current password and a new password of 8 to 72 bytes.', 400);
}
if (auth_rate_limited('password-change', (string) $me['id'])) json_error('Too many attempts. Please try later.', 429);
try {
    $changed = change_account_password((int) $me['id'], $current, $new);
} catch (Throwable) {
    json_error('Could not update your password. Please try again.', 503);
}
if (!$changed) json_error('Your current password is incorrect or the account is no longer active.', 400);
json_response(true, (object) [], 'Password updated. Sign in again on each device.');
