<?php
/** GET /api/nannies/earnings.php — escrow-aware summary for the logged-in nanny. */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();
require_api_role($me, 'nanny');

// Release anything past the 48h grace period before computing totals, matching
// the web app's auto_release_stale_payments() cron/job behaviour.
auto_release_stale_payments(48);

$nid = (int) $me['id'];

$totalsStmt = db()->prepare(
    "SELECT
        COALESCE(SUM(CASE WHEN p.payout_status IN ('held','released') THEN p.amount ELSE 0 END), 0) AS total_earnings,
        COALESCE(SUM(CASE WHEN p.payout_status = 'held' THEN p.amount ELSE 0 END), 0) AS held_earnings,
        COALESCE(SUM(CASE WHEN p.payout_status = 'released' THEN p.amount ELSE 0 END), 0) AS released_earnings
     FROM payments p JOIN bookings b ON b.id = p.booking_id WHERE b.nanny_id = :nid"
);
$totalsStmt->execute(['nid' => $nid]);
$totals = $totalsStmt->fetch();

$completedStmt = db()->prepare("SELECT COUNT(*) FROM bookings WHERE nanny_id = :nid AND status = 'completed'");
$completedStmt->execute(['nid' => $nid]);
$completed = (int) $completedStmt->fetchColumn();

$recentStmt = db()->prepare(
    'SELECT p.*, b.booking_ref FROM payments p JOIN bookings b ON b.id = p.booking_id
     WHERE b.nanny_id = :nid ORDER BY p.created_at DESC LIMIT 20'
);
$recentStmt->execute(['nid' => $nid]);

$byDayStmt = db()->prepare(
    "SELECT DATE(p.created_at) AS d, SUM(p.amount) AS total FROM payments p JOIN bookings b ON b.id = p.booking_id
     WHERE b.nanny_id = :nid AND p.created_at > DATE_SUB(NOW(), INTERVAL 30 DAY) GROUP BY DATE(p.created_at) ORDER BY d"
);
$byDayStmt->execute(['nid' => $nid]);
$byDay = [];
foreach ($byDayStmt->fetchAll() as $r) {
    $byDay[$r['d']] = (float) $r['total'];
}

json_response(true, [
    'totalEarnings' => (float) $totals['total_earnings'],
    'heldEarnings' => (float) $totals['held_earnings'],
    'releasedEarnings' => (float) $totals['released_earnings'],
    'completedJobs' => $completed,
    'recentPayments' => array_map(fn($p) => [
        'id' => (int) $p['id'], 'bookingId' => (int) $p['booking_id'], 'bookingRef' => $p['booking_ref'],
        'amount' => (float) $p['amount'], 'method' => $p['method'], 'transactionId' => $p['transaction_id'],
        'status' => $p['status'], 'payoutStatus' => $p['payout_status'], 'releasedAt' => $p['released_at'],
        'createdAt' => $p['created_at'],
    ], $recentStmt->fetchAll()),
    'earningsByDay' => $byDay,
]);
