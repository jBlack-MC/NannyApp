<?php
/** POST /api/messages/mark_read.php { with } — marks all messages from `with` as read. */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();

$otherId = (int) (json_body()['with'] ?? 0);
db()->prepare('UPDATE chat_messages SET is_read = 1 WHERE sender_id = :s AND receiver_id = :r AND is_read = 0')
    ->execute(['s' => $otherId, 'r' => $me['id']]);

json_response(true, null);
