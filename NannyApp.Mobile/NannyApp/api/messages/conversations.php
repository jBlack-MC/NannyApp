<?php
/** GET /api/messages/conversations.php — list of distinct conversation partners
 * with the last message and unread count, newest first. */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();
$uid = (int) $me['id'];

$stmt = db()->prepare(
    "SELECT other_id, MAX(created_at) AS last_at FROM (
        SELECT CASE WHEN sender_id = :uid1 THEN receiver_id ELSE sender_id END AS other_id, created_at
        FROM chat_messages WHERE sender_id = :uid2 OR receiver_id = :uid3
    ) t GROUP BY other_id ORDER BY last_at DESC"
);
$stmt->execute(['uid1' => $uid, 'uid2' => $uid, 'uid3' => $uid]);
$partners = $stmt->fetchAll();

$result = [];
foreach ($partners as $p) {
    $otherId = (int) $p['other_id'];

    $lastStmt = db()->prepare(
        'SELECT content, created_at FROM chat_messages
         WHERE (sender_id = :a AND receiver_id = :b) OR (sender_id = :b2 AND receiver_id = :a2)
         ORDER BY created_at DESC LIMIT 1'
    );
    $lastStmt->execute(['a' => $uid, 'b' => $otherId, 'b2' => $uid, 'a2' => $otherId]);
    $last = $lastStmt->fetch();

    $unreadStmt = db()->prepare('SELECT COUNT(*) FROM chat_messages WHERE sender_id = :s AND receiver_id = :r AND is_read = 0');
    $unreadStmt->execute(['s' => $otherId, 'r' => $uid]);
    $unread = (int) $unreadStmt->fetchColumn();

    $userStmt = db()->prepare('SELECT full_name, profile_image FROM users WHERE id = :id');
    $userStmt->execute(['id' => $otherId]);
    $user = $userStmt->fetch();
    if (!$user) continue;

    $result[] = [
        'withUserId' => $otherId, 'withUserName' => $user['full_name'], 'withUserPhotoUrl' => media_url($user['profile_image']),
        'lastMessage' => $last['content'] ?? '', 'lastMessageAt' => $last['created_at'] ?? '', 'unreadCount' => $unread,
    ];
}

json_response(true, $result);
