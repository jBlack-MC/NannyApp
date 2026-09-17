<?php
/** POST /api/reviews/create.php { bookingId, nannyId, rating, comment } */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();
require_api_role($me, 'parent');

$b = json_body();
$bookingId = (int) ($b['bookingId'] ?? 0);
$nannyId = (int) ($b['nannyId'] ?? 0);
$rating = max(1, min(5, (int) ($b['rating'] ?? 0)));
$comment = trim((string) ($b['comment'] ?? ''));

$stmt = db()->prepare("SELECT * FROM bookings WHERE id = :id AND parent_id = :pid AND status = 'completed'");
$stmt->execute(['id' => $bookingId, 'pid' => $me['id']]);
if (!$stmt->fetch()) {
    json_error('You can only review a completed booking.', 409);
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
// Recomputes nanny_profiles.average_rating from all reviews — same helper used by the web app.
recompute_rating($nannyId);
notify($nannyId, 'New review', 'A parent left you a new review.', '/nanny/reviews.php');

json_response(true, [
    'id' => $reviewId, 'bookingId' => $bookingId, 'reviewerId' => (int) $me['id'], 'reviewerName' => $me['full_name'],
    'nannyId' => $nannyId, 'rating' => $rating, 'comment' => $comment, 'createdAt' => date('Y-m-d H:i:s'),
]);
