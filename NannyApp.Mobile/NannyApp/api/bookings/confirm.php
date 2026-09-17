<?php
/** POST /api/bookings/confirm.php { bookingId } — parent confirms the job was
 * done, moving the booking to completed and releasing the escrowed payment. */
require_once __DIR__ . '/../_bootstrap.php';
require_once __DIR__ . '/_serialize.php';
$me = require_api_auth();
require_api_role($me, 'parent');

$bookingId = (int) (json_body()['bookingId'] ?? 0);
$stmt = db()->prepare('SELECT * FROM bookings WHERE id = :id AND parent_id = :pid');
$stmt->execute(['id' => $bookingId, 'pid' => $me['id']]);
$booking = $stmt->fetch();

if (!$booking) json_error('Booking not found.', 404);
if ($booking['status'] !== 'in_progress' || $booking['checked_out_at'] === null) {
    json_error('This booking cannot be confirmed yet.', 409);
}

db()->beginTransaction();
try {
    db()->prepare("UPDATE bookings SET status = 'completed', parent_confirmed_at = NOW() WHERE id = :id")
        ->execute(['id' => $bookingId]);
    db()->prepare("UPDATE payments SET payout_status = 'released', released_at = NOW() WHERE booking_id = :id AND status = 'paid'")
        ->execute(['id' => $bookingId]);
    db()->commit();
} catch (Throwable $e) {
    db()->rollBack();
    json_error('Could not confirm this booking. Please try again.', 500);
}

notify((int) $booking['nanny_id'], 'Payment released', 'The parent confirmed the session. Your payment has been released.', '/nanny/earnings.php');

$stmt = db()->prepare(BOOKING_SELECT_SQL . ' WHERE b.id = :id');
$stmt->execute(['id' => $bookingId]);
json_response(true, serialize_booking($stmt->fetch(), $me));
