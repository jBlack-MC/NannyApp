<?php
/** POST /api/reviews/create.php { bookingId, nannyId, rating, comment } */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();
require_api_role($me, 'parent');

if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') json_error('Method not allowed.', 405);
$b = json_body();
$bookingId = filter_var($b['bookingId'] ?? null, FILTER_VALIDATE_INT, ['options' => ['min_range' => 1]]);
$submittedNannyId = filter_var($b['nannyId'] ?? null, FILTER_VALIDATE_INT, ['options' => ['min_range' => 1]]);
$rating = filter_var($b['rating'] ?? null, FILTER_VALIDATE_INT, ['options' => ['min_range' => 1, 'max_range' => 5]]);
if ($bookingId === false || $submittedNannyId === false || $rating === false || !is_string($b['comment'] ?? '')) {
    json_error('Provide a valid booking, nanny and rating from 1 to 5.');
}
$comment = trim($b['comment'] ?? '');
if (strlen($comment) > 5000) json_error('Review comment is too long.');

$stmt = db()->prepare("SELECT * FROM bookings WHERE id = :id AND parent_id = :pid AND status = 'completed'");
$stmt->execute(['id' => $bookingId, 'pid' => $me['id']]);
$booking = $stmt->fetch();
if (!$booking) {
    json_error('You can only review a completed booking.', 409);
}

$nannyId = (int) $booking['nanny_id'];
if ($submittedNannyId !== $nannyId) {
    json_error('The review must be for the nanny assigned to this booking.', 409);
}

try {
    $stmt = db()->prepare(
        'INSERT INTO reviews (booking_id, reviewer_id, nanny_id, rating, comment) VALUES (:bid, :rid, :nid, :rating, :comment)'
    );
    $stmt->execute(['bid' => $bookingId, 'rid' => $me['id'], 'nid' => $nannyId, 'rating' => $rating, 'comment' => $comment ?: null]);
} catch (Throwable $e) {
    json_error('You have already reviewed this booking.', 409);
}

$reviewId = (int) db()->lastInsertId();
// Recomputes nanny_profiles.average_rating from all reviews â€” same helper used by the web app.
recompute_rating($nannyId);
notify($nannyId, 'New review', 'A parent left you a new review.', '/nanny/reviews.php');

json_response(true, [
    'id' => $reviewId, 'bookingId' => $bookingId, 'reviewerId' => (int) $me['id'], 'reviewerName' => $me['full_name'],
    'nannyId' => $nannyId, 'rating' => $rating, 'comment' => $comment, 'createdAt' => date('Y-m-d H:i:s'),
]);
