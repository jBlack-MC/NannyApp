<?php
/** GET /api/payments/verify.php?reference=... — server-side verification against
 * Paystack, then marks the payment paid + escrow held. */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();

$reference = (string) ($_GET['reference'] ?? '');
if ($reference === '') json_error('Missing payment reference.');

$stmt = db()->prepare('SELECT * FROM payments WHERE transaction_id = :ref');
$stmt->execute(['ref' => $reference]);
$payment = $stmt->fetch();
if (!$payment) json_error('Payment not found.', 404);

$paystackSecret = defined('PAYSTACK_SECRET_KEY') ? PAYSTACK_SECRET_KEY : (getenv('PAYSTACK_SECRET_KEY') ?: null);
if (!$paystackSecret) json_error('Payments are not configured on this server yet.', 500);

$ch = curl_init("https://api.paystack.co/transaction/verify/" . rawurlencode($reference));
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_HTTPHEADER => ["Authorization: Bearer $paystackSecret"],
]);
$response = curl_exec($ch);
curl_close($ch);
$data = json_decode((string) $response, true);

$verified = ($data['data']['status'] ?? null) === 'success';
if ($verified) {
    db()->prepare("UPDATE payments SET status = 'paid', payout_status = 'held' WHERE id = :id")
        ->execute(['id' => $payment['id']]);
    $bookingStmt = db()->prepare('SELECT parent_id, nanny_id FROM bookings WHERE id = :id');
    $bookingStmt->execute(['id' => $payment['booking_id']]);
    if ($b = $bookingStmt->fetch()) {
        notify((int) $b['nanny_id'], 'Payment received', 'Payment for a booking has been received and is held securely.', '/nanny/bookings.php');
    }
} else {
    db()->prepare("UPDATE payments SET status = 'failed' WHERE id = :id")->execute(['id' => $payment['id']]);
    json_error('Payment could not be verified. Please try again.', 402);
}

$stmt = db()->prepare(
    'SELECT p.*, b.booking_ref FROM payments p JOIN bookings b ON b.id = p.booking_id WHERE p.id = :id'
);
$stmt->execute(['id' => $payment['id']]);
$p = $stmt->fetch();

json_response(true, [
    'id' => (int) $p['id'], 'bookingId' => (int) $p['booking_id'], 'bookingRef' => $p['booking_ref'],
    'amount' => (float) $p['amount'], 'method' => $p['method'], 'transactionId' => $p['transaction_id'],
    'status' => $p['status'], 'payoutStatus' => $p['payout_status'], 'releasedAt' => $p['released_at'],
    'createdAt' => $p['created_at'],
]);
