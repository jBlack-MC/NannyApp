<?php
/** POST /api/messages/send.php { receiverId, content } */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();

$b = json_body();
$receiverId = (int) ($b['receiverId'] ?? 0);
$content = trim((string) ($b['content'] ?? ''));
if ($content === '' || $receiverId <= 0) json_error('Message cannot be empty.');

// Reuses send_chat_message() from functions.php, which also creates the
// "New message" notification for the receiver.
$result = send_chat_message((int) $me['id'], $receiverId, $content);
if (!($result['ok'] ?? false)) {
    json_error($result['error'] ?? 'Could not send message.', 400);
}

json_response(true, [
    'id' => (int) $result['id'], 'senderId' => (int) $me['id'], 'receiverId' => $receiverId,
    'content' => $result['content'], 'isRead' => false, 'createdAt' => $result['created_at'],
]);
