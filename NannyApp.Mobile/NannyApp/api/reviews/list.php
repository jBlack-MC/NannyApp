<?php
/** GET /api/reviews/list.php?nanny_id=123 — thin alias of nannies/reviews.php. */
require_once __DIR__ . '/../_bootstrap.php';
require_api_auth();

$id = (int) ($_GET['nanny_id'] ?? 0);
$stmt = db()->prepare(
    'SELECT rv.*, u.full_name AS reviewer_name FROM reviews rv JOIN users u ON u.id = rv.reviewer_id
     WHERE rv.nanny_id = :id ORDER BY rv.created_at DESC LIMIT 200'
);
$stmt->execute(['id' => $id]);

json_response(true, array_map(fn($r) => [
    'id' => (int) $r['id'], 'bookingId' => (int) $r['booking_id'], 'reviewerId' => (int) $r['reviewer_id'],
    'reviewerName' => $r['reviewer_name'], 'nannyId' => (int) $r['nanny_id'], 'rating' => (int) $r['rating'],
    'comment' => $r['comment'], 'createdAt' => $r['created_at'],
], $stmt->fetchAll()));
