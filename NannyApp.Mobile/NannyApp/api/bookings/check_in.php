<?php
/** POST /api/bookings/check_in.php { bookingId, checkInCode } — nanny enters the
 * parent's PIN. Max 5 attempts before the booking is locked and flagged (matches
 * the handshake described in nanny/bookings.php). */
require_once __DIR__ . '/../_bootstrap.php';
require_once __DIR__ . '/_serialize.php';
$me = require_api_auth();
require_api_role($me, 'nanny');

$b = json_body();
$bookingId = (int) ($b['bookingId'] ?? 0);
$code = strtoupper(trim((string) ($b['checkInCode'] ?? '')));

$stmt = db()->prepare('SELECT * FROM bookings WHERE id = :id AND nanny_id = :nid');
$stmt->execute(['id' => $bookingId, 'nid' => $me['id']]);
$booking = $stmt->fetch();

if (!$booking) json_error('Booking not found.', 404);
if ($booking['status'] !== 'confirmed') json_error('This booking is not ready for check-in.', 409);
if ((int) $booking['check_in_attempts'] >= 5) {
    json_error('Too many incorrect attempts. Please contact support to verify this booking.', 423);
}

if (strtoupper((string) $booking['check_in_code']) !== $code) {
    db()->prepare('UPDATE bookings SET check_in_attempts = check_in_attempts + 1 WHERE id = :id')->execute(['id' => $bookingId]);
    $remaining = 5 - ((int) $booking['check_in_attempts'] + 1);
    json_error("Incorrect PIN. $remaining attempt(s) remaining.", 400);
}

db()->prepare("UPDATE bookings SET status = 'in_progress', checked_in_at = NOW() WHERE id = :id")->execute(['id' => $bookingId]);
notify((int) $booking['parent_id'], 'Nanny checked in', 'Your nanny has checked in and the session has started.', '/parent/bookings.php');

$stmt = db()->prepare(BOOKING_SELECT_SQL . ' WHERE b.id = :id');
$stmt->execute(['id' => $bookingId]);
json_response(true, serialize_booking($stmt->fetch(), $me));
