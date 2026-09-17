<?php
/** POST /api/admin/booking_status.php { bookingId, status } — manual override for
 * admin dispute resolution / support cases. */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();
require_api_role($me, 'admin');

$b = json_body();
$bookingId = (int) ($b['bookingId'] ?? 0);
$status = $b['status'] ?? '';
$valid = ['pending', 'confirmed', 'in_progress', 'completed', 'rejected', 'cancelled', 'disputed'];
if (!in_array($status, $valid, true)) json_error('Invalid status.');

db()->prepare('UPDATE bookings SET status = :status WHERE id = :id')->execute(['status' => $status, 'id' => $bookingId]);

$stmt = db()->prepare('SELECT parent_id, nanny_id FROM bookings WHERE id = :id');
$stmt->execute(['id' => $bookingId]);
if ($row = $stmt->fetch()) {
    notify((int) $row['parent_id'], 'Booking updated', "An admin updated this booking's status to \"$status\".", '/parent/bookings.php');
    notify((int) $row['nanny_id'], 'Booking updated', "An admin updated this booking's status to \"$status\".", '/nanny/bookings.php');
}

json_response(true, null);
