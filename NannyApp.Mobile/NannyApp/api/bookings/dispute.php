<?php
/** POST /api/bookings/dispute.php { bookingId, reason } — parent reports an issue
 * instead of confirming. Freezes the escrowed payment for admin review. */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();
require_api_role($me, 'parent');

$b = json_body();
$bookingId = (int) ($b['bookingId'] ?? 0);
$reason = trim((string) ($b['reason'] ?? ''));

if ($reason === '') json_error('Please describe the issue.');

$stmt = db()->prepare('SELECT * FROM bookings WHERE id = :id AND parent_id = :pid');
$stmt->execute(['id' => $bookingId, 'pid' => $me['id']]);
$booking = $stmt->fetch();

if (!$booking) json_error('Booking not found.', 404);
if (!in_array($booking['status'], ['in_progress', 'completed'], true)) {
    json_error('This booking cannot be disputed.', 409);
}

db()->prepare("UPDATE bookings SET status = 'disputed', dispute_reason = :reason, disputed_at = NOW() WHERE id = :id")
    ->execute(['reason' => $reason, 'id' => $bookingId]);

create_support_ticket($me['full_name'], $me['email'], "Dispute: booking #$bookingId", $reason, 'booking', (int) $me['id']);
notify((int) $booking['nanny_id'], 'Booking disputed', 'The parent has raised a dispute for this booking. Our team will review it.', '/nanny/bookings.php');

json_response(true, null, 'Your report has been submitted. Our team will review it shortly.');
