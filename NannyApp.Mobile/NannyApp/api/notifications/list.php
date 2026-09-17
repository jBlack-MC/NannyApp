<?php
/** GET /api/notifications/list.php */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();

$stmt = db()->prepare('SELECT * FROM notifications WHERE user_id = :id ORDER BY created_at DESC LIMIT 100');
$stmt->execute(['id' => $me['id']]);

json_response(true, array_map(fn($n) => [
    'id' => (int) $n['id'], 'title' => $n['title'], 'message' => $n['message'], 'url' => $n['url'],
    'isRead' => (bool) $n['is_read'], 'createdAt' => $n['created_at'],
], $stmt->fetchAll()));
