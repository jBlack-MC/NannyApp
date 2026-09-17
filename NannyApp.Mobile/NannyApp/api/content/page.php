<?php
/** GET /api/content/page.php?key=faq — reads the page_content CMS table.
 * Deliberately public (no auth) since the Welcome screen links to About/FAQ/etc.
 * before the person has logged in or registered. */
require_once __DIR__ . '/../_bootstrap.php';

$key = (string) ($_GET['key'] ?? '');
$stmt = db()->prepare('SELECT * FROM page_content WHERE page_key = :key');
$stmt->execute(['key' => $key]);
$row = $stmt->fetch();

if (!$row) {
    // Fall back to a friendly placeholder instead of a 404 for pages the admin
    // hasn't populated yet (About/Services/Safety/Pricing/Community/Resources
    // aren't seeded by the original schema — only faq/terms/privacy are).
    json_response(true, ['pageKey' => $key, 'title' => ucfirst($key), 'body' => 'This page has not been published yet. Please check back soon.']);
}

json_response(true, ['pageKey' => $row['page_key'], 'title' => $row['title'], 'body' => $row['body']]);
