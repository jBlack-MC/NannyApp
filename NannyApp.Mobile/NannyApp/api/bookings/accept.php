<?php
/** POST /api/bookings/accept.php { bookingId } — nanny accepts a pending request.
 * Generates the one-time check-in PIN shown only to the parent. */
require_once __DIR__ . '/../_bootstrap.php';
require_once __DIR__ . '/_serialize.php';
$me = require_api_auth();
require_api_role($me, 'nanny');

$bookingId = (int) (json_body()['bookingId'] ?? 0);
$stmt = db()->prepare('SELECT * FROM bookings WHERE id = :id AND nanny_id = :nid');
$stmt->execute(['id' => $bookingId, 'nid' => $me['id']]);
$booking = $stmt->fetch();

if (!$booking) json_error('Booking not found.', 404);
if ($booking['status'] !== 'pending') json_error('This booking can no longer be accepted.', 409);

$code = generate_check_in_code();
db()->prepare("UPDATE bookings SET status = 'confirmed', check_in_code = :code WHERE id = :id")
    ->execute(['code' => $code, 'id' => $bookingId]);

notify((int) $booking['parent_id'], 'Booking confirmed', 'Your nanny confirmed the booking. Your check-in PIN is ready.', '/parent/bookings.php');

$stmt = db()->prepare(BOOKING_SELECT_SQL . ' WHERE b.id = :id');
$stmt->execute(['id' => $bookingId]);
json_response(true, serialize_booking($stmt->fetch(), $me));
