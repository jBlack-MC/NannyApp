<?php
/** POST /api/admin/verify_document.php { id, approve, notes } — approves/rejects one
 * nanny_portfolio row (request #26/#19). */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();
require_api_role($me, 'admin');

$b = json_body();
$id = (int) ($b['id'] ?? 0);
$approve = !empty($b['approve']);

db()->prepare('UPDATE nanny_portfolio SET admin_verified = :v WHERE id = :id')
    ->execute(['v' => $approve ? 1 : 0, 'id' => $id]);

$stmt = db()->prepare('SELECT nanny_id, title FROM nanny_portfolio WHERE id = :id');
$stmt->execute(['id' => $id]);
if ($row = $stmt->fetch()) {
    notify((int) $row['nanny_id'], $approve ? 'Document verified' : 'Document needs attention',
        ($approve ? 'Your document "' : 'Your document "') . $row['title'] . ($approve ? '" has been verified.' : '" could not be verified. Please re-upload.'),
        '/nanny/portfolio.php');
}

json_response(true, null);
