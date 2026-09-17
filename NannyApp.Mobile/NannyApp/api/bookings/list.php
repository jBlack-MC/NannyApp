<?php
/** GET /api/bookings/list.php — bookings for the logged-in parent or nanny. */
require_once __DIR__ . '/../_bootstrap.php';
require_once __DIR__ . '/_serialize.php';
$me = require_api_auth();

$column = $me['role'] === 'nanny' ? 'b.nanny_id' : 'b.parent_id';
$stmt = db()->prepare(BOOKING_SELECT_SQL . " WHERE $column = :id ORDER BY b.date_time DESC");
$stmt->execute(['id' => $me['id']]);

json_response(true, array_map(fn($b) => serialize_booking($b, $me), $stmt->fetchAll()));
