<?php
/** GET /api/messages/poll.php?with=123&since_id=45 — new messages since the given id.
 * Called every few seconds by the chat screen (request #21: efficient polling). */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();
$otherId = (int) ($_GET['with'] ?? 0);
$sinceId = (int) ($_GET['since_id'] ?? 0);

$stmt = db()->prepare(
    'SELECT * FROM chat_messages WHERE ((sender_id = :a AND receiver_id = :b) OR (sender_id = :b2 AND receiver_id = :a2))
     AND id > :since ORDER BY created_at ASC LIMIT 100'
);
$stmt->execute(['a' => $me['id'], 'b' => $otherId, 'b2' => $me['id'], 'a2' => $otherId, 'since' => $sinceId]);

json_response(true, array_map(fn($m) => [
    'id' => (int) $m['id'], 'senderId' => (int) $m['sender_id'], 'receiverId' => (int) $m['receiver_id'],
    'content' => $m['content'], 'isRead' => (bool) $m['is_read'], 'createdAt' => $m['created_at'],
], $stmt->fetchAll()));
