<?php
/** GET /api/nannies/detail.php?id=123  (id=0 or omitted means "me", nanny role only) */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();

$id = (int) ($_GET['id'] ?? 0);
if ($id === 0) {
    require_api_role($me, 'nanny');
    $id = (int) $me['id'];
}

$stmt = db()->prepare(
    "SELECT u.id AS user_id, u.full_name, u.created_at AS member_since, np.*,
            (SELECT COUNT(*) FROM reviews r WHERE r.nanny_id = u.id) AS review_count
     FROM users u JOIN nanny_profiles np ON np.user_id = u.id WHERE u.id = :id"
);
$stmt->execute(['id' => $id]);
$row = $stmt->fetch();

if (!$row) {
    json_error('Nanny not found.', 404);
}

// Track profile views the same way nanny profile pages do on the web, but only
// count views from someone other than the nanny themselves.
if ((int) $me['id'] !== $id) {
    db()->prepare('UPDATE nanny_profiles SET profile_views = profile_views + 1 WHERE user_id = :id')->execute(['id' => $id]);
}

json_response(true, [
    'userId' => (int) $row['user_id'], 'fullName' => $row['full_name'], 'photoUrl' => media_url($row['photo_url']),
    'bannerImage' => media_url($row['banner_image']), 'bio' => $row['bio'], 'gender' => $row['gender'],
    'experienceYears' => (int) $row['experience_years'], 'hourlyRate' => (float) $row['hourly_rate'],
    'location' => $row['location'], 'skills' => $row['skills'], 'languages' => $row['languages'],
    'qualifications' => $row['qualifications'], 'specialisations' => $row['specialisations'],
    'availability' => $row['availability'], 'verificationStatus' => $row['verification_status'],
    'averageRating' => (float) $row['average_rating'], 'reviewCount' => (int) $row['review_count'],
    'profileViews' => (int) $row['profile_views'], 'memberSince' => $row['member_since'],
]);
