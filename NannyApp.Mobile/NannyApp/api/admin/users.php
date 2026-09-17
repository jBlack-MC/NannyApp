<?php
/** GET /api/admin/users.php?role=&q= */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();
require_api_role($me, 'admin');

$where = ['1=1'];
$params = [];
if (!empty($_GET['role'])) { $where[] = 'role = :role'; $params['role'] = $_GET['role']; }
if (!empty($_GET['q'])) { $where[] = '(full_name LIKE :q OR email LIKE :q)'; $params['q'] = '%' . $_GET['q'] . '%'; }

$stmt = db()->prepare('SELECT * FROM users WHERE ' . implode(' AND ', $where) . ' ORDER BY created_at DESC LIMIT 200');
$stmt->execute($params);

json_response(true, array_map(fn($u) => [
    'id' => (int) $u['id'], 'fullName' => $u['full_name'], 'email' => $u['email'], 'phone' => $u['phone'],
    'role' => $u['role'], 'status' => $u['status'], 'emailVerified' => (bool) $u['email_verified'],
    'profileImage' => media_url($u['profile_image']), 'dateOfBirth' => $u['date_of_birth'], 'address' => $u['address'],
    'gender' => $u['gender'], 'createdAt' => $u['created_at'],
], $stmt->fetchAll()));
