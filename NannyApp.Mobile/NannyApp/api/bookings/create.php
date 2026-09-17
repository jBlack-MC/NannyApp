<?php
/** POST /api/bookings/create.php — parent creates a booking (status=pending).
 * Payment happens afterwards via /api/payments/initialize.php + verify.php. */
require_once __DIR__ . '/../_bootstrap.php';
require_once __DIR__ . '/_serialize.php';
$me = require_api_auth();
require_api_role($me, 'parent');

$b = json_body();
$nannyId = (int) ($b['nannyId'] ?? 0);
$dateTime = (string) ($b['dateTime'] ?? '');
$duration = (float) ($b['duration'] ?? 0);
$address = (string) ($b['address'] ?? '');
$childrenIds = $b['childrenIds'] ?? [];

if ($nannyId <= 0 || $dateTime === '' || $duration <= 0 || $address === '' || empty($childrenIds)) {
    json_error('Please complete every step of the booking before continuing.');
}

// Mirrors nanny_has_booking_conflict() used by parent/book.php to stop double-booking a nanny.
if (nanny_has_booking_conflict($nannyId, $dateTime, $duration)) {
    json_error('This nanny already has a booking that overlaps with the selected time. Please choose another slot.', 409);
}

$childrenDetails = [];
if (!empty($childrenIds)) {
    $placeholders = implode(',', array_fill(0, count($childrenIds), '?'));
    $stmt = db()->prepare("SELECT name, age FROM children WHERE id IN ($placeholders) AND parent_id = ?");
    $stmt->execute([...$childrenIds, $me['id']]);
    foreach ($stmt->fetchAll() as $c) {
        $childrenDetails[] = $c['name'] . ($c['age'] ? " ({$c['age']})" : '');
    }
}

db()->beginTransaction();
try {
    $stmt = db()->prepare(
        'INSERT INTO bookings (parent_id, nanny_id, date_time, duration, location, notes, children_details, booking_address, status)
         VALUES (:pid, :nid, :dt, :dur, :loc, :notes, :cd, :addr, "pending")'
    );
    $stmt->execute([
        'pid' => $me['id'], 'nid' => $nannyId, 'dt' => $dateTime, 'dur' => $duration,
        'loc' => $address, 'notes' => $b['notes'] ?? null, 'cd' => implode(', ', $childrenDetails), 'addr' => $address,
    ]);
    $bookingId = (int) db()->lastInsertId();
    db()->prepare('UPDATE bookings SET booking_ref = :ref WHERE id = :id')
        ->execute(['ref' => generate_booking_ref($bookingId), 'id' => $bookingId]);
    db()->commit();
} catch (Throwable $e) {
    db()->rollBack();
    json_error('Could not create the booking. Please try again.', 500);
}

notify($nannyId, 'New booking request', 'You have a new booking request. Review and respond.', '/nanny/bookings.php');

$stmt = db()->prepare(BOOKING_SELECT_SQL . ' WHERE b.id = :id');
$stmt->execute(['id' => $bookingId]);
json_response(true, serialize_booking($stmt->fetch(), $me));
