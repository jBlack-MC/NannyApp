<?php
/** GET /api/content/page.php?key=faq — reads the page_content CMS table.
 * Deliberately public (no auth) since the Welcome screen links to About/FAQ/etc.
 * before the person has logged in or registered. */
require_once __DIR__ . '/../_bootstrap.php';

$key = (string) ($_GET['key'] ?? '');
$pages = json_decode(file_get_contents(__DIR__ . '/../../../../NannyApp.Shared/content/public-pages.json'), true, 512, JSON_THROW_ON_ERROR);
if (!isset($pages[$key])) {
    json_error('Page not found.', 404);
}
$row = false;
try {
    $stmt = db()->prepare('SELECT * FROM page_content WHERE page_key = :key');
    $stmt->execute(['key' => $key]);
    $row = $stmt->fetch();
} catch (PDOException $e) {
    // Existing installs may not have the optional CMS table yet.
    if ($e->getCode() !== '42S02') {
        error_log('Public content lookup failed: ' . $e->getMessage());
        json_error('Content is temporarily unavailable.', 503);
    }
}

if (!$row || trim((string) $row['body']) === '') {
    json_response(true, $pages[$key]);
}

json_response(true, ['pageKey' => $row['page_key'], 'title' => $row['title'], 'body' => $row['body']]);
