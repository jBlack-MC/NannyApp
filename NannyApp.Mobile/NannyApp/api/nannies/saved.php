<?php
/** GET /api/nannies/saved.php — parent's saved nannies with full nanny profile embedded. */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();
require_api_role($me, 'parent');

$stmt = db()->prepare(
    "SELECT sn.id, sn.nanny_id, sn.created_at, u.full_name, u.created_at AS member_since, np.*,
            (SELECT COUNT(*) FROM reviews r WHERE r.nanny_id = u.id) AS review_count
     FROM saved_nannies sn
     JOIN users u ON u.id = sn.nanny_id
     JOIN nanny_profiles np ON np.user_id = u.id
     WHERE sn.parent_id = :pid ORDER BY sn.created_at DESC"
);
$stmt->execute(['pid' => $me['id']]);

json_response(true, array_map(fn($r) => [
    'id' => (int) $r['id'], 'nannyId' => (int) $r['nanny_id'], 'createdAt' => $r['created_at'],
    'nanny' => [
        'userId' => (int) $r['nanny_id'], 'fullName' => $r['full_name'], 'photoUrl' => media_url($r['photo_url']),
        'bannerImage' => media_url($r['banner_image']), 'bio' => $r['bio'], 'gender' => $r['gender'],
        'experienceYears' => (int) $r['experience_years'], 'hourlyRate' => (float) $r['hourly_rate'],
        'location' => $r['location'], 'skills' => $r['skills'], 'languages' => $r['languages'],
        'qualifications' => $r['qualifications'], 'specialisations' => $r['specialisations'],
        'availability' => $r['availability'], 'verificationStatus' => $r['verification_status'],
        'averageRating' => (float) $r['average_rating'], 'reviewCount' => (int) $r['review_count'],
        'profileViews' => (int) $r['profile_views'], 'memberSince' => $r['member_since'],
    ],
], $stmt->fetchAll()));
