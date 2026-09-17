<?php
/** GET /api/nannies/search.php?q=&location=&min_rate=&max_rate=&min_experience=&min_rating=&verified_only=&slot=&sort=
 * Public-ish endpoint (requires login like the rest of the app) listing nanny_profiles joined to users. */
require_once __DIR__ . '/../_bootstrap.php';
require_api_auth();

$where = ["u.role = 'nanny'"];
$params = [];

if (!empty($_GET['q'])) {
    $where[] = '(u.full_name LIKE :q OR np.location LIKE :q OR np.skills LIKE :q)';
    $params['q'] = '%' . $_GET['q'] . '%';
}
if (!empty($_GET['location'])) {
    $where[] = 'np.location LIKE :location';
    $params['location'] = '%' . $_GET['location'] . '%';
}
if (isset($_GET['min_rate']) && $_GET['min_rate'] !== '') {
    $where[] = 'np.hourly_rate >= :min_rate';
    $params['min_rate'] = (float) $_GET['min_rate'];
}
if (isset($_GET['max_rate']) && $_GET['max_rate'] !== '') {
    $where[] = 'np.hourly_rate <= :max_rate';
    $params['max_rate'] = (float) $_GET['max_rate'];
}
if (isset($_GET['min_experience']) && $_GET['min_experience'] !== '') {
    $where[] = 'np.experience_years >= :min_exp';
    $params['min_exp'] = (int) $_GET['min_experience'];
}
if (isset($_GET['min_rating']) && $_GET['min_rating'] !== '') {
    $where[] = 'np.average_rating >= :min_rating';
    $params['min_rating'] = (float) $_GET['min_rating'];
}
if (!empty($_GET['verified_only']) && $_GET['verified_only'] === 'true') {
    $where[] = "np.verification_status = 'verified'";
}
if (!empty($_GET['slot'])) {
    $where[] = 'EXISTS (SELECT 1 FROM availability_slots s WHERE s.nanny_id = u.id AND s.slot = :slot)';
    $params['slot'] = $_GET['slot'];
}

$orderBy = match ($_GET['sort'] ?? 'rating') {
    'experience' => 'np.experience_years DESC',
    'price_low' => 'np.hourly_rate ASC',
    'price_high' => 'np.hourly_rate DESC',
    default => 'np.average_rating DESC',
};

$sql = "SELECT u.id AS user_id, u.full_name, u.created_at AS member_since, np.* ,
               (SELECT COUNT(*) FROM reviews r WHERE r.nanny_id = u.id) AS review_count
        FROM users u JOIN nanny_profiles np ON np.user_id = u.id
        WHERE " . implode(' AND ', $where) . "
        ORDER BY $orderBy LIMIT 100";

$stmt = db()->prepare($sql);
$stmt->execute($params);
$rows = $stmt->fetchAll();

json_response(true, array_map('serialize_nanny_row', $rows));

function serialize_nanny_row(array $r): array {
    return [
        'userId' => (int) $r['user_id'], 'fullName' => $r['full_name'], 'photoUrl' => media_url($r['photo_url']),
        'bannerImage' => media_url($r['banner_image']), 'bio' => $r['bio'], 'gender' => $r['gender'],
        'experienceYears' => (int) $r['experience_years'], 'hourlyRate' => (float) $r['hourly_rate'],
        'location' => $r['location'], 'skills' => $r['skills'], 'languages' => $r['languages'],
        'qualifications' => $r['qualifications'], 'specialisations' => $r['specialisations'],
        'availability' => $r['availability'], 'verificationStatus' => $r['verification_status'],
        'averageRating' => (float) $r['average_rating'], 'reviewCount' => (int) $r['review_count'],
        'profileViews' => (int) $r['profile_views'], 'memberSince' => $r['member_since'] ?? null,
    ];
}
