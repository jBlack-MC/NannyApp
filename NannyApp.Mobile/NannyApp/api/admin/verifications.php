<?php
/** GET /api/admin/verifications.php — nannies pending verification. */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();
require_api_role($me, 'admin');

$stmt = db()->query(
    "SELECT u.id AS user_id, u.full_name, u.created_at AS member_since, np.*,
            (SELECT COUNT(*) FROM reviews r WHERE r.nanny_id = u.id) AS review_count
     FROM users u JOIN nanny_profiles np ON np.user_id = u.id
     WHERE np.verification_status = 'pending' ORDER BY u.created_at ASC"
);

json_response(true, array_map(fn($r) => [
    'userId' => (int) $r['user_id'], 'fullName' => $r['full_name'], 'photoUrl' => media_url($r['photo_url']),
    'bannerImage' => media_url($r['banner_image']), 'bio' => $r['bio'], 'gender' => $r['gender'],
    'experienceYears' => (int) $r['experience_years'], 'hourlyRate' => (float) $r['hourly_rate'],
    'location' => $r['location'], 'skills' => $r['skills'], 'languages' => $r['languages'],
    'qualifications' => $r['qualifications'], 'specialisations' => $r['specialisations'],
    'availability' => $r['availability'], 'verificationStatus' => $r['verification_status'],
    'averageRating' => (float) $r['average_rating'], 'reviewCount' => (int) $r['review_count'],
    'profileViews' => (int) $r['profile_views'], 'memberSince' => $r['member_since'],
], $stmt->fetchAll()));
