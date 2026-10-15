<?php
/**
 * GET /api/nannies/viewers.php — who viewed the signed-in nanny's profile.
 * Every nanny sees the count; Nanny-App Plus nannies also see which families viewed and when.
 */
require_once __DIR__ . '/../_bootstrap.php';
require_once __DIR__ . '/../../../../NannyApp.Shared/config/subscriptions.php';

$me = require_api_auth();
require_api_role($me, 'nanny');
$id = (int) $me['id'];

$count = db()->prepare('SELECT COUNT(*) FROM profile_views WHERE nanny_id = ? AND viewed_at > DATE_SUB(NOW(), INTERVAL 30 DAY)');
$count->execute([$id]);
$unlocked = plus_active(db(), $id);
$recent = [];
if ($unlocked) {
    $stmt = db()->prepare('SELECT u.id, u.full_name, u.profile_image, MAX(v.viewed_at) AS viewed_at
        FROM profile_views v JOIN users u ON u.id = v.viewer_id
        WHERE v.nanny_id = ? AND v.viewed_at > DATE_SUB(NOW(), INTERVAL 30 DAY)
        GROUP BY u.id, u.full_name, u.profile_image ORDER BY viewed_at DESC LIMIT 30');
    $stmt->execute([$id]);
    $recent = array_map(fn($r) => ['userId' => (int) $r['id'], 'name' => $r['full_name'],
        'photoUrl' => media_url($r['profile_image']), 'viewedAt' => $r['viewed_at']], $stmt->fetchAll());
}
json_response(true, ['viewsLast30Days' => (int) $count->fetchColumn(), 'locked' => !$unlocked, 'recent' => $recent]);
