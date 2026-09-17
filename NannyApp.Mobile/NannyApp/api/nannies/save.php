<?php
/** POST /api/nannies/save.php { nanny_id, save: 1|0 } -> { saved: bool } */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();
require_api_role($me, 'parent');

$b = json_body();
$nannyId = (int) ($b['nanny_id'] ?? 0);
$save = !empty($b['save']);

if ($save) {
    db()->prepare('INSERT IGNORE INTO saved_nannies (parent_id, nanny_id) VALUES (:pid, :nid)')
        ->execute(['pid' => $me['id'], 'nid' => $nannyId]);
} else {
    db()->prepare('DELETE FROM saved_nannies WHERE parent_id = :pid AND nanny_id = :nid')
        ->execute(['pid' => $me['id'], 'nid' => $nannyId]);
}

json_response(true, ['saved' => $save]);
