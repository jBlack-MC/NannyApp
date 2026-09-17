<?php
/** POST /api/bookings/check_out.php { bookingId } — nanny ends the session.
 * Status stays in_progress until the parent confirms (escrow not released yet). */
require_once __DIR__ . '/../_bootstrap.php';
require_once __DIR__ . '/_serialize.php';
$me = require_api_auth();
require_api_role($me, 'nanny');

$bookingId = (int) (json_body()['bookingId'] ?? 0);
$stmt = db()->prepare('SELECT * FROM bookings WHERE id = :id AND nanny_id = :nid');
$stmt->execute(['id' => $bookingId, 'nid' => $me['id']]);
$booking = $stmt->fetch();

if (!$booking) json_error('Booking not found.', 404);
if ($booking['status'] !== 'in_progress' || $booking['checked_out_at'] !== null) {
    json_error('This booking cannot be checked out right now.', 409);
}

db()->prepare('UPDATE bookings SET checked_out_at = NOW() WHERE id = :id')->execute(['id' => $bookingId]);
notify((int) $booking['parent_id'], 'Session complete', 'Your nanny has checked out. Please confirm the session to release payment.', '/parent/bookings.php');

$stmt = db()->prepare(BOOKING_SELECT_SQL . ' WHERE b.id = :id');
$stmt->execute(['id' => $bookingId]);
json_response(true, serialize_booking($stmt->fetch(), $me));
