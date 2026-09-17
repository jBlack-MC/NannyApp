<?php
/** DELETE /api/children/delete.php?id=123 */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();
require_api_role($me, 'parent');

$id = (int) ($_GET['id'] ?? 0);
db()->prepare('DELETE FROM children WHERE id = :id AND parent_id = :pid')->execute(['id' => $id, 'pid' => $me['id']]);

json_response(true, null);
