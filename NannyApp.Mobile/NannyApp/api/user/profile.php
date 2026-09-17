<?php
/** GET /api/user/profile.php -> current user
 *  PUT /api/user/profile.php { fullName, phone, dateOfBirth, address, gender } -> updated user */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();

function serialize_user(array $u): array {
    return [
        'id' => (int) $u['id'], 'fullName' => $u['full_name'], 'email' => $u['email'], 'phone' => $u['phone'],
        'role' => $u['role'], 'status' => $u['status'], 'emailVerified' => (bool) $u['email_verified'],
        'profileImage' => media_url($u['profile_image']), 'dateOfBirth' => $u['date_of_birth'],
        'address' => $u['address'], 'gender' => $u['gender'], 'createdAt' => $u['created_at'],
    ];
}

$method = $_SERVER['REQUEST_METHOD'] ?? 'GET';

if ($method === 'GET') {
    json_response(true, serialize_user($me));
}

if ($method === 'PUT') {
    $b = json_body();
    db()->prepare(
        'UPDATE users SET full_name = :name, phone = :phone, date_of_birth = :dob, address = :address, gender = :gender WHERE id = :id'
    )->execute([
        'name' => $b['fullName'] ?? $me['full_name'], 'phone' => $b['phone'] ?? $me['phone'],
        'dob' => $b['dateOfBirth'] ?? $me['date_of_birth'], 'address' => $b['address'] ?? $me['address'],
        'gender' => $b['gender'] ?? $me['gender'], 'id' => $me['id'],
    ]);
    $stmt = db()->prepare('SELECT * FROM users WHERE id = :id');
    $stmt->execute(['id' => $me['id']]);
    json_response(true, serialize_user($stmt->fetch()));
}

json_error('Method not allowed.', 405);
