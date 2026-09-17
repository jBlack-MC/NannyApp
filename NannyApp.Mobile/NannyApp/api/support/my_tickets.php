<?php
/** GET /api/support/my_tickets.php */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();

$stmt = db()->prepare('SELECT * FROM support_tickets WHERE user_id = :id ORDER BY created_at DESC');
$stmt->execute(['id' => $me['id']]);

json_response(true, array_map('serialize_ticket', $stmt->fetchAll()));

function serialize_ticket(array $t): array {
    return [
        'id' => (int) $t['id'], 'userId' => $t['user_id'] !== null ? (int) $t['user_id'] : null,
        'name' => $t['name'], 'email' => $t['email'], 'category' => $t['category'], 'subject' => $t['subject'],
        'message' => $t['message'], 'status' => $t['status'], 'adminNotes' => $t['admin_notes'],
        'createdAt' => $t['created_at'], 'updatedAt' => $t['updated_at'],
    ];
}
