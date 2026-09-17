<?php
/** POST /api/bookings/reject.php { bookingId } — nanny declines a pending request. */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();
require_api_role($me, 'nanny');

$bookingId = (int) (json_body()['bookingId'] ?? 0);
$stmt = db()->prepare('SELECT * FROM bookings WHERE id = :id AND nanny_id = :nid');
$stmt->execute(['id' => $bookingId, 'nid' => $me['id']]);
$booking = $stmt->fetch();

if (!$booking) json_error('Booking not found.', 404);
if ($booking['status'] !== 'pending') json_error('This booking can no longer be rejected.', 409);

db()->prepare("UPDATE bookings SET status = 'rejected' WHERE id = :id")->execute(['id' => $bookingId]);
db()->prepare("UPDATE payments SET status = 'refunded', payout_status = 'refunded' WHERE booking_id = :id AND status = 'paid'")
    ->execute(['id' => $bookingId]);

notify((int) $booking['parent_id'], 'Booking declined', 'Unfortunately the nanny could not accept this booking. Any payment has been refunded.', '/parent/bookings.php');

json_response(true, null);
