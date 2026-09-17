<?php
/** POST /api/bookings/cancel.php { bookingId } — parent cancels while pending/confirmed. */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();
require_api_role($me, 'parent');

$bookingId = (int) (json_body()['bookingId'] ?? 0);
$stmt = db()->prepare('SELECT * FROM bookings WHERE id = :id AND parent_id = :pid');
$stmt->execute(['id' => $bookingId, 'pid' => $me['id']]);
$booking = $stmt->fetch();

if (!$booking) json_error('Booking not found.', 404);
if (!in_array($booking['status'], ['pending', 'confirmed'], true)) {
    json_error('This booking can no longer be cancelled.', 409);
}

db()->prepare("UPDATE bookings SET status = 'cancelled' WHERE id = :id")->execute(['id' => $bookingId]);
db()->prepare("UPDATE payments SET status = 'refunded', payout_status = 'refunded' WHERE booking_id = :id AND status = 'paid'")
    ->execute(['id' => $bookingId]);

notify((int) $booking['nanny_id'], 'Booking cancelled', 'The parent has cancelled this booking.', '/nanny/bookings.php');

json_response(true, null);
