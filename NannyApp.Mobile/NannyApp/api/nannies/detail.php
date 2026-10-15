<?php
/** GET /api/nannies/detail.php?id=123  (id=0 or omitted means "me", nanny role only) */
require_once __DIR__ . '/../_bootstrap.php';
require_once __DIR__ . '/../../../../NannyApp.Shared/config/subscriptions.php';
$me = require_api_auth();

$id = (int) ($_GET['id'] ?? 0);
if ($id === 0) {
    require_api_role($me, 'nanny');
    $id = (int) $me['id'];
}

$stmt = db()->prepare(
    "SELECT u.id AS user_id, u.full_name, u.created_at AS member_since, np.*, u.profile_image,
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
    if ($me['role'] === 'parent') plus_record_profile_view(db(), $id, (int) $me['id']);
}

$fullAccess = (int) $me['id'] === $id || $me['role'] === 'admin' || plus_active(db(), (int) $me['id']);
$verification = null;
if ($fullAccess) {
    // Which documents an administrator has checked: type, title and date, never the files themselves.
    $docs = db()->prepare('SELECT type, title, created_at FROM nanny_portfolio WHERE nanny_id = ? AND admin_verified = 1 ORDER BY created_at DESC');
    $docs->execute([$id]);
    $verification = array_map(fn($d) => ['type' => $d['type'], 'title' => $d['title'], 'addedAt' => $d['created_at']], $docs->fetchAll());
}

json_response(true, [
    'userId' => (int) $row['user_id'], 'fullName' => $row['full_name'], 'photoUrl' => media_url($row['photo_url'] ?: ($row['profile_image'] ?? null)),
    'bannerImage' => media_url($row['banner_image']), 'bio' => $row['bio'], 'gender' => $row['gender'],
    'experienceYears' => (int) $row['experience_years'], 'hourlyRate' => (float) $row['hourly_rate'],
    'location' => $row['location'], 'skills' => $row['skills'], 'languages' => $row['languages'],
    'qualifications' => $fullAccess ? $row['qualifications'] : null, 'specialisations' => $fullAccess ? $row['specialisations'] : null,
    'extendedLocked' => !$fullAccess, 'verifiedDocuments' => $verification, 'featured' => plus_active(db(), $id),
    'availability' => $row['availability'], 'verificationStatus' => $row['verification_status'],
    'averageRating' => (float) $row['average_rating'], 'reviewCount' => (int) $row['review_count'],
    'profileViews' => (int) $row['profile_views'], 'memberSince' => $row['member_since'],
]);
