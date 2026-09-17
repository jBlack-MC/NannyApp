<?php
/** POST /api/payments/initialize.php { booking_id } -> Paystack authorization_url.
 * The secret key lives ONLY on this server (config/paystack.php, not committed) —
 * it is never sent to the Android app (request #23/#35). */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();
require_api_role($me, 'parent');

$bookingId = (int) (json_body()['booking_id'] ?? 0);
$stmt = db()->prepare('SELECT b.*, np.hourly_rate FROM bookings b JOIN nanny_profiles np ON np.user_id = b.nanny_id WHERE b.id = :id AND b.parent_id = :pid');
$stmt->execute(['id' => $bookingId, 'pid' => $me['id']]);
$booking = $stmt->fetch();
if (!$booking) json_error('Booking not found.', 404);

$amount = round(((float) $booking['duration']) * ((float) $booking['hourly_rate']), 2);
$reference = 'NANNY_' . $bookingId . '_' . bin2hex(random_bytes(6));

// Create/refresh the payments row as pending until Paystack confirms it.
db()->prepare(
    'INSERT INTO payments (booking_id, amount, method, transaction_id, status)
     VALUES (:bid, :amount, "card", :ref, "pending")
     ON DUPLICATE KEY UPDATE amount = VALUES(amount), transaction_id = VALUES(transaction_id), status = "pending"'
)->execute(['bid' => $bookingId, 'amount' => $amount, 'ref' => $reference]);

$paystackSecret = defined('PAYSTACK_SECRET_KEY') ? PAYSTACK_SECRET_KEY : (getenv('PAYSTACK_SECRET_KEY') ?: null);
if (!$paystackSecret) {
    json_error('Payments are not configured on this server yet. Set PAYSTACK_SECRET_KEY in config/paystack.php.', 500);
}

$ch = curl_init('https://api.paystack.co/transaction/initialize');
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_POST => true,
    CURLOPT_HTTPHEADER => ["Authorization: Bearer $paystackSecret", 'Content-Type: application/json'],
    CURLOPT_POSTFIELDS => json_encode([
        'email' => $me['email'],
        'amount' => (int) round($amount * 100), // kobo/cents
        'reference' => $reference,
        'callback_url' => 'nannyapp://payment-callback',
    ]),
]);
$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

$data = json_decode((string) $response, true);
if ($httpCode !== 200 || empty($data['status'])) {
    json_error('Could not start the payment. Please try again.', 502);
}

json_response(true, [
    'authorizationUrl' => $data['data']['authorization_url'],
    'reference' => $reference,
    'accessCode' => $data['data']['access_code'] ?? null,
]);
