<?php
/** GET /api/admin/bookings.php?status=&q= — all bookings across the platform. */
require_once __DIR__ . '/../_bootstrap.php';
require_once __DIR__ . '/../bookings/_serialize.php';
$me = require_api_auth();
require_api_role($me, 'admin');

$where = ['1=1'];
$params = [];
if (!empty($_GET['status'])) { $where[] = 'b.status = :status'; $params['status'] = $_GET['status']; }
if (!empty($_GET['q'])) {
    $where[] = '(b.booking_ref LIKE :q OR pu.full_name LIKE :q OR nu.full_name LIKE :q)';
    $params['q'] = '%' . $_GET['q'] . '%';
}

$stmt = db()->prepare(BOOKING_SELECT_SQL . ' WHERE ' . implode(' AND ', $where) . ' ORDER BY b.created_at DESC LIMIT 300');
$stmt->execute($params);

json_response(true, array_map(fn($b) => serialize_booking($b, $me), $stmt->fetchAll()));
