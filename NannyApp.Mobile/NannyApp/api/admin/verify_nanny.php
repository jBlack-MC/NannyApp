<?php
/** POST /api/admin/verify_nanny.php { nannyId, notes } */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();
require_api_role($me, 'admin');

$b = json_body();
$nannyId = (int) ($b['nannyId'] ?? 0);

db()->prepare("UPDATE nanny_profiles SET verification_status = 'verified' WHERE user_id = :id")->execute(['id' => $nannyId]);
notify($nannyId, 'You are verified!', 'Congratulations — your NannyApp profile has been verified.', '/nanny/profile.php');

json_response(true, null);
