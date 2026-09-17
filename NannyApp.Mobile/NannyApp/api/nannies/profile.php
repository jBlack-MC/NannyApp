<?php
/** PUT /api/nannies/profile.php — nanny updates their own profile fields. */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();
require_api_role($me, 'nanny');

if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'PUT') {
    json_error('Method not allowed.', 405);
}

$b = json_body();
db()->prepare(
    'UPDATE nanny_profiles SET bio = :bio, experience_years = :exp, hourly_rate = :rate, location = :loc,
        skills = :skills, languages = :lang, qualifications = :qual, specialisations = :spec, availability = :avail
     WHERE user_id = :uid'
)->execute([
    'bio' => $b['bio'] ?? null, 'exp' => $b['experienceYears'] ?? 0, 'rate' => $b['hourlyRate'] ?? 0,
    'loc' => $b['location'] ?? null, 'skills' => $b['skills'] ?? null, 'lang' => $b['languages'] ?? null,
    'qual' => $b['qualifications'] ?? null, 'spec' => $b['specialisations'] ?? null,
    'avail' => $b['availability'] ?? null, 'uid' => $me['id'],
]);

$stmt = db()->prepare(
    "SELECT u.id AS user_id, u.full_name, u.created_at AS member_since, np.*,
            (SELECT COUNT(*) FROM reviews r WHERE r.nanny_id = u.id) AS review_count
     FROM users u JOIN nanny_profiles np ON np.user_id = u.id WHERE u.id = :id"
);
$stmt->execute(['id' => $me['id']]);
$row = $stmt->fetch();

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
