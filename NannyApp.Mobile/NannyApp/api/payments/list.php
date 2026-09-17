<?php
/** GET /api/payments/list.php — payments visible to the logged-in parent or nanny. */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();

$column = $me['role'] === 'nanny' ? 'b.nanny_id' : 'b.parent_id';
$stmt = db()->prepare(
    "SELECT p.*, b.booking_ref FROM payments p JOIN bookings b ON b.id = p.booking_id
     WHERE $column = :id ORDER BY p.created_at DESC"
);
$stmt->execute(['id' => $me['id']]);

json_response(true, array_map(fn($p) => [
    'id' => (int) $p['id'], 'bookingId' => (int) $p['booking_id'], 'bookingRef' => $p['booking_ref'],
    'amount' => (float) $p['amount'], 'method' => $p['method'], 'transactionId' => $p['transaction_id'],
    'status' => $p['status'], 'payoutStatus' => $p['payout_status'], 'releasedAt' => $p['released_at'],
    'createdAt' => $p['created_at'],
], $stmt->fetchAll()));
