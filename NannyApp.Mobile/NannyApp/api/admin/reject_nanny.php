<?php
/** POST /api/admin/reject_nanny.php { nannyId, notes } */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();
require_api_role($me, 'admin');

$b = json_body();
$nannyId = (int) ($b['nannyId'] ?? 0);
$notes = trim((string) ($b['notes'] ?? ''));

db()->prepare("UPDATE nanny_profiles SET verification_status = 'rejected' WHERE user_id = :id")->execute(['id' => $nannyId]);
notify($nannyId, 'Verification update', $notes !== '' ? $notes : 'Your verification could not be approved. Please review your documents and try again.', '/nanny/portfolio.php');

json_response(true, null);
