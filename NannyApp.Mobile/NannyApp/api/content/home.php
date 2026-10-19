<?php
/** GET /api/content/home.php — what the website's home page shows: gallery photos, top-rated
 * verified nannies and recent testimonials. Public (no auth) like the website's home page, so the
 * Welcome screen can show it before the person signs in. Each part is optional; a failure leaves
 * that list empty and the app falls back to its bundled photos. */
require_once __DIR__ . '/../_bootstrap.php';

header('Cache-Control: public, max-age=300');

/** Absolute URL of a public website image, with each path segment encoded (gallery names have spaces). */
function home_asset_url(string $path): string
{
    return web_root_url() . implode('/', array_map('rawurlencode', explode('/', $path)));
}

// Same choice as NannyApp.Web/index.php: up to eight gallery photos, else the fixed home-page set.
$gallery = [];
$galleryDir = __DIR__ . '/../../../../NannyApp.Web/assets/img/gallery';
foreach (is_dir($galleryDir) ? (scandir($galleryDir) ?: []) : [] as $entry) {
    if (preg_match('/\.(?:jpe?g|png|webp|gif)$/i', $entry) && is_file($galleryDir . '/' . $entry)) {
        $gallery[] = 'assets/img/gallery/' . $entry;
    }
}
if ($gallery) {
    shuffle($gallery);
    $gallery = array_slice($gallery, 0, 8);
} else {
    $gallery = array_map(fn(string $f) => 'assets/img/' . $f, ['hero-nanny-SMceVYR7.jpg', 'download.jpg', 'download (1).jpg', 'download (2).jpg', 'download (3).jpg', 'download (4).jpg']);
}

$nannies = [];
try {
    $rows = db()->query(
        "SELECT u.id, u.full_name, u.profile_image, p.photo_url, p.location, p.hourly_rate, p.average_rating, p.experience_years,
                (SELECT COUNT(*) FROM reviews r WHERE r.nanny_id = u.id) AS review_count
         FROM nanny_profiles p
         JOIN users u ON u.id = p.user_id
         WHERE p.verification_status = 'verified'
         ORDER BY p.average_rating DESC, p.experience_years DESC
         LIMIT 6"
    )->fetchAll();
    foreach ($rows as $r) {
        $nannies[] = [
            'userId' => (int) $r['id'], 'fullName' => $r['full_name'],
            'photoUrl' => media_url($r['photo_url'] ?: ($r['profile_image'] ?? null)),
            'location' => $r['location'], 'hourlyRate' => (float) $r['hourly_rate'],
            'averageRating' => (float) $r['average_rating'], 'reviewCount' => (int) $r['review_count'],
            'experienceYears' => (int) $r['experience_years'],
        ];
    }
} catch (Throwable $e) {
    log_exception($e, 'home_nannies_failed');
}

$testimonials = [];
try {
    $rows = db()->query(
        "SELECT p.full_name AS parent_name, n.full_name AS nanny_name, r.comment, r.rating
         FROM reviews r
         JOIN users p ON p.id = r.reviewer_id
         JOIN users n ON n.id = r.nanny_id
         WHERE COALESCE(r.comment, '') <> ''
         ORDER BY r.id DESC
         LIMIT 8"
    )->fetchAll();
    foreach ($rows as $r) {
        $testimonials[] = [
            // First names only: the app shows these to people who have not signed in.
            'parentName' => strtok((string) $r['parent_name'], ' ') ?: '', 'nannyName' => strtok((string) $r['nanny_name'], ' ') ?: '',
            'comment' => $r['comment'], 'rating' => (int) $r['rating'],
        ];
    }
} catch (Throwable $e) {
    log_exception($e, 'home_testimonials_failed');
}

json_response(true, [
    'galleryImages' => array_map('home_asset_url', $gallery),
    'topNannies' => $nannies,
    'testimonials' => $testimonials,
]);
