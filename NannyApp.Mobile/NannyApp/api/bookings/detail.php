<?php
/** GET /api/bookings/detail.php?id=123 — must belong to the caller (parent, nanny, or admin). */
require_once __DIR__ . '/../_bootstrap.php';
require_once __DIR__ . '/_serialize.php';
$me = require_api_auth();

$id = (int) ($_GET['id'] ?? 0);
$stmt = db()->prepare(BOOKING_SELECT_SQL . ' WHERE b.id = :id');
$stmt->execute(['id' => $id]);
$booking = $stmt->fetch();

if (!$booking) {
    json_error('Booking not found.', 404);
}
$owns = (int) $booking['parent_id'] === (int) $me['id'] || (int) $booking['nanny_id'] === (int) $me['id'];
if (!$owns && $me['role'] !== 'admin') {
    json_error('You are not authorized to view this booking.', 403);
}

json_response(true, serialize_booking($booking, $me));
