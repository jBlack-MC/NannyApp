<?php
/** GET /api/admin/support.php?status= — all support tickets. */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();
require_api_role($me, 'admin');

$where = ['1=1'];
$params = [];
if (!empty($_GET['status'])) { $where[] = 'status = :status'; $params['status'] = $_GET['status']; }

$stmt = db()->prepare('SELECT * FROM support_tickets WHERE ' . implode(' AND ', $where) . ' ORDER BY created_at DESC LIMIT 300');
$stmt->execute($params);

json_response(true, array_map(fn($t) => [
    'id' => (int) $t['id'], 'userId' => $t['user_id'] !== null ? (int) $t['user_id'] : null,
    'name' => $t['name'], 'email' => $t['email'], 'category' => $t['category'], 'subject' => $t['subject'],
    'message' => $t['message'], 'status' => $t['status'], 'adminNotes' => $t['admin_notes'],
    'createdAt' => $t['created_at'], 'updatedAt' => $t['updated_at'],
], $stmt->fetchAll()));
