<?php
/** POST /api/admin/support_update.php { ticketId, status, adminNotes } */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();
require_api_role($me, 'admin');

$b = json_body();
$ticketId = (int) ($b['ticketId'] ?? 0);
$status = $b['status'] ?? '';
$notes = $b['adminNotes'] ?? null;
$valid = ['open', 'in_progress', 'resolved', 'closed'];
if (!in_array($status, $valid, true)) json_error('Invalid status.');

db()->prepare('UPDATE support_tickets SET status = :status, admin_notes = :notes WHERE id = :id')
    ->execute(['status' => $status, 'notes' => $notes, 'id' => $ticketId]);

$stmt = db()->prepare('SELECT user_id FROM support_tickets WHERE id = :id');
$stmt->execute(['id' => $ticketId]);
if (($row = $stmt->fetch()) && $row['user_id']) {
    notify((int) $row['user_id'], 'Support ticket updated', "Your support ticket status is now \"$status\".", '/support.php');
}

json_response(true, null);
