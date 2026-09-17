<?php
/** POST /api/bookings/reschedule.php { bookingId, dateTime } */
require_once __DIR__ . '/../_bootstrap.php';
require_once __DIR__ . '/_serialize.php';
$me = require_api_auth();
require_api_role($me, 'parent');

$b = json_body();
$bookingId = (int) ($b['bookingId'] ?? 0);
$newDateTime = (string) ($b['dateTime'] ?? '');

$stmt = db()->prepare('SELECT * FROM bookings WHERE id = :id AND parent_id = :pid');
$stmt->execute(['id' => $bookingId, 'pid' => $me['id']]);
$booking = $stmt->fetch();

if (!$booking) json_error('Booking not found.', 404);
if (!in_array($booking['status'], ['pending', 'confirmed'], true)) {
    json_error('This booking can no longer be rescheduled.', 409);
}
// Conflict check duplicated inline (excluding this booking itself) since
// nanny_has_booking_conflict() doesn't support an exclusion — it's designed
// for brand-new bookings only.
$start = new DateTimeImmutable($newDateTime);
$end = $start->add(new DateInterval('PT' . (int) round(((float) $booking['duration']) * 3600) . 'S'));
$conflictStmt = db()->prepare(
    'SELECT COUNT(*) FROM bookings WHERE nanny_id = :nid AND id != :bid AND status IN ("pending","confirmed")
     AND date_time < :end AND TIMESTAMPADD(SECOND, ROUND(duration * 3600), date_time) > :start'
);
$conflictStmt->execute([
    'nid' => $booking['nanny_id'], 'bid' => $bookingId,
    'end' => $end->format('Y-m-d H:i:s'), 'start' => $start->format('Y-m-d H:i:s'),
]);
if ((int) $conflictStmt->fetchColumn() > 0) {
    json_error('The nanny is not available at that time.', 409);
}

db()->prepare('UPDATE bookings SET date_time = :dt WHERE id = :id')->execute(['dt' => $newDateTime, 'id' => $bookingId]);
notify((int) $booking['nanny_id'], 'Booking rescheduled', 'The parent has rescheduled this booking.', '/nanny/bookings.php');

$stmt = db()->prepare(BOOKING_SELECT_SQL . ' WHERE b.id = :id');
$stmt->execute(['id' => $bookingId]);
json_response(true, serialize_booking($stmt->fetch(), $me));
