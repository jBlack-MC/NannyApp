<?php
/** GET /api/admin/dashboard.php — reproduces admin/dashboard.php's KPI cards. */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();
require_api_role($me, 'admin');

auto_release_stale_payments(48);

$totalUsers = (int) db()->query("SELECT COUNT(*) FROM users")->fetchColumn();
$totalParents = (int) db()->query("SELECT COUNT(*) FROM users WHERE role='parent'")->fetchColumn();
$totalNannies = (int) db()->query("SELECT COUNT(*) FROM users WHERE role='nanny'")->fetchColumn();
$verifiedNannies = (int) db()->query("SELECT COUNT(*) FROM nanny_profiles WHERE verification_status='verified'")->fetchColumn();
$pendingVerifications = (int) db()->query("SELECT COUNT(*) FROM nanny_profiles WHERE verification_status='pending'")->fetchColumn();
$pendingDocuments = (int) db()->query("SELECT COUNT(*) FROM nanny_portfolio WHERE admin_verified=0")->fetchColumn();
$totalBookings = (int) db()->query("SELECT COUNT(*) FROM bookings")->fetchColumn();
$pendingBookings = (int) db()->query("SELECT COUNT(*) FROM bookings WHERE status='pending'")->fetchColumn();
$totalRevenue = (float) db()->query("SELECT COALESCE(SUM(amount),0) FROM payments WHERE status='paid'")->fetchColumn();
$openTickets = (int) db()->query("SELECT COUNT(*) FROM support_tickets WHERE status IN ('open','in_progress')")->fetchColumn();

$recentUsersStmt = db()->query("SELECT * FROM users ORDER BY created_at DESC LIMIT 10");
$recentUsers = array_map(fn($u) => [
    'id' => (int) $u['id'], 'fullName' => $u['full_name'], 'email' => $u['email'], 'phone' => $u['phone'],
    'role' => $u['role'], 'status' => $u['status'], 'emailVerified' => (bool) $u['email_verified'],
    'profileImage' => media_url($u['profile_image']), 'dateOfBirth' => $u['date_of_birth'], 'address' => $u['address'],
    'gender' => $u['gender'], 'createdAt' => $u['created_at'],
], $recentUsersStmt->fetchAll());

require_once __DIR__ . '/../bookings/_serialize.php';
$recentBookingsStmt = db()->query(BOOKING_SELECT_SQL . ' ORDER BY b.created_at DESC LIMIT 10');
$recentBookings = array_map(fn($b) => serialize_booking($b, $me), $recentBookingsStmt->fetchAll());

$topNanniesStmt = db()->query(
    "SELECT u.id AS user_id, u.full_name, u.created_at AS member_since, np.*,
            (SELECT COUNT(*) FROM reviews r WHERE r.nanny_id = u.id) AS review_count,
            COALESCE(SUM(CASE WHEN p.status='paid' THEN p.amount ELSE 0 END), 0) AS total_earnings
     FROM users u
     JOIN nanny_profiles np ON np.user_id = u.id
     LEFT JOIN bookings b ON b.nanny_id = u.id
     LEFT JOIN payments p ON p.booking_id = b.id
     WHERE u.role = 'nanny'
     GROUP BY u.id ORDER BY total_earnings DESC LIMIT 10"
);
$topNannies = array_map(fn($r) => [
    'nanny' => [
        'userId' => (int) $r['user_id'], 'fullName' => $r['full_name'], 'photoUrl' => media_url($r['photo_url']),
        'bannerImage' => media_url($r['banner_image']), 'bio' => $r['bio'], 'gender' => $r['gender'],
        'experienceYears' => (int) $r['experience_years'], 'hourlyRate' => (float) $r['hourly_rate'],
        'location' => $r['location'], 'skills' => $r['skills'], 'languages' => $r['languages'],
        'qualifications' => $r['qualifications'], 'specialisations' => $r['specialisations'],
        'availability' => $r['availability'], 'verificationStatus' => $r['verification_status'],
        'averageRating' => (float) $r['average_rating'], 'reviewCount' => (int) $r['review_count'],
        'profileViews' => (int) $r['profile_views'], 'memberSince' => $r['member_since'],
    ],
    'totalEarnings' => (float) $r['total_earnings'],
], $topNanniesStmt->fetchAll());

$revenueByDayStmt = db()->query(
    "SELECT DATE(created_at) d, SUM(amount) total FROM payments WHERE status='paid' AND created_at > DATE_SUB(NOW(), INTERVAL 30 DAY) GROUP BY DATE(created_at) ORDER BY d"
);
$revenueByDay = [];
foreach ($revenueByDayStmt->fetchAll() as $r) { $revenueByDay[$r['d']] = (float) $r['total']; }

$regByDayStmt = db()->query(
    "SELECT DATE(created_at) d, COUNT(*) c FROM users WHERE created_at > DATE_SUB(NOW(), INTERVAL 30 DAY) GROUP BY DATE(created_at) ORDER BY d"
);
$regByDay = [];
foreach ($regByDayStmt->fetchAll() as $r) { $regByDay[$r['d']] = (int) $r['c']; }

$byStatusStmt = db()->query("SELECT status, COUNT(*) c FROM bookings GROUP BY status");
$byStatus = [];
foreach ($byStatusStmt->fetchAll() as $r) { $byStatus[$r['status']] = (int) $r['c']; }

$topLocStmt = db()->query("SELECT location, COUNT(*) c FROM nanny_profiles WHERE location IS NOT NULL AND location <> '' GROUP BY location ORDER BY c DESC LIMIT 10");
$topLocations = [];
foreach ($topLocStmt->fetchAll() as $r) { $topLocations[$r['location']] = (int) $r['c']; }

json_response(true, [
    'totalUsers' => $totalUsers, 'totalParents' => $totalParents, 'totalNannies' => $totalNannies,
    'verifiedNannies' => $verifiedNannies, 'totalBookings' => $totalBookings, 'pendingBookings' => $pendingBookings,
    'totalRevenue' => $totalRevenue, 'pendingVerifications' => $pendingVerifications, 'pendingDocuments' => $pendingDocuments,
    'openSupportTickets' => $openTickets, 'recentUsers' => $recentUsers, 'recentBookings' => $recentBookings,
    'topEarningNannies' => $topNannies, 'revenueByDay' => $revenueByDay, 'registrationsByDay' => $regByDay,
    'bookingsByStatus' => $byStatus, 'topLocations' => $topLocations,
]);
