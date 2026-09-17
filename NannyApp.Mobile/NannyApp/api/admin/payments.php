<?php
/** GET /api/admin/payments.php?status= — all payments across the platform. */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();
require_api_role($me, 'admin');

$where = ['1=1'];
$params = [];
if (!empty($_GET['status'])) { $where[] = 'p.status = :status'; $params['status'] = $_GET['status']; }

$stmt = db()->prepare(
    'SELECT p.*, b.booking_ref FROM payments p JOIN bookings b ON b.id = p.booking_id
     WHERE ' . implode(' AND ', $where) . ' ORDER BY p.created_at DESC LIMIT 300'
);
$stmt->execute($params);

json_response(true, array_map(fn($p) => [
    'id' => (int) $p['id'], 'bookingId' => (int) $p['booking_id'], 'bookingRef' => $p['booking_ref'],
    'amount' => (float) $p['amount'], 'method' => $p['method'], 'transactionId' => $p['transaction_id'],
    'status' => $p['status'], 'payoutStatus' => $p['payout_status'], 'releasedAt' => $p['released_at'],
    'createdAt' => $p['created_at'],
], $stmt->fetchAll()));
