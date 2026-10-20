<?php
require_once __DIR__ . '/../config/config.php';
require_role('admin');

$users = db()->query(
    "SELECT
        SUM(CASE WHEN role='parent' THEN 1 ELSE 0 END) AS parents,
        SUM(CASE WHEN role='nanny' THEN 1 ELSE 0 END) AS nannies,
        SUM(CASE WHEN role='admin' THEN 1 ELSE 0 END) AS admins,
        SUM(CASE WHEN status='suspended' THEN 1 ELSE 0 END) AS suspended,
        COUNT(*) AS total
     FROM users"
)->fetch();

$bookings = db()->query(
    "SELECT
        SUM(CASE WHEN status='pending' THEN 1 ELSE 0 END) AS pending,
        SUM(CASE WHEN status='confirmed' THEN 1 ELSE 0 END) AS confirmed,
        SUM(CASE WHEN status='completed' THEN 1 ELSE 0 END) AS completed,
        SUM(CASE WHEN status='rejected' THEN 1 ELSE 0 END) AS rejected,
        SUM(CASE WHEN status='cancelled' THEN 1 ELSE 0 END) AS cancelled,
        COUNT(*) AS total
     FROM bookings"
)->fetch();

$revenue = db()->query(
    "SELECT
        COALESCE(SUM(CASE WHEN status='paid' THEN amount END),0) AS collected,
        COALESCE(SUM(CASE WHEN status='pending' THEN amount END),0) AS outstanding
     FROM payments"
)->fetch();

$verif = db()->query(
    "SELECT
        SUM(CASE WHEN verification_status='pending' THEN 1 ELSE 0 END) AS pending,
        SUM(CASE WHEN verification_status='verified' THEN 1 ELSE 0 END) AS verified,
        SUM(CASE WHEN verification_status='rejected' THEN 1 ELSE 0 END) AS rejected
     FROM nanny_profiles"
)->fetch();

$apk = db()->query(
    "SELECT COUNT(*) AS total,
            SUM(CASE WHEN created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) THEN 1 ELSE 0 END) AS last_week,
            SUM(CASE WHEN source='apk-share' THEN 1 ELSE 0 END) AS shared_from_app
     FROM apk_download_events"
)->fetch();

$pageTitle = 'Reports';
require __DIR__ . '/../includes/header.php';

/** Tiny helper to render a labelled count row. */
function report_row(string $label, $value): void
{
    echo '<tr><td>' . e($label) . '</td><td class="text-right"><strong>' . (int)$value . '</strong></td></tr>';
}
?>
<h1>Reports &amp; analytics</h1>

<div class="grid grid-2 section">
    <div class="card">
        <h2>User report</h2>
        <table class="table">
            <?php
                report_row('Parents', $users['parents']);
                report_row('Nannies', $users['nannies']);
                report_row('Administrators', $users['admins']);
                report_row('Suspended', $users['suspended']);
                report_row('Total users', $users['total']);
            ?>
        </table>
    </div>

    <div class="card">
        <h2>Booking report</h2>
        <table class="table">
            <?php
                report_row('Pending', $bookings['pending']);
                report_row('Confirmed', $bookings['confirmed']);
                report_row('Completed', $bookings['completed']);
                report_row('Rejected', $bookings['rejected']);
                report_row('Cancelled', $bookings['cancelled']);
                report_row('Total bookings', $bookings['total']);
            ?>
        </table>
    </div>

    <div class="card">
        <h2>Verification report</h2>
        <table class="table">
            <?php
                report_row('Pending', $verif['pending']);
                report_row('Verified', $verif['verified']);
                report_row('Rejected', $verif['rejected']);
            ?>
        </table>
    </div>

    <div class="card">
        <h2>Revenue report</h2>
        <table class="table">
            <tr><td>Collected (paid)</td><td class="text-right"><strong>R<?= number_format((float)$revenue['collected'], 2) ?></strong></td></tr>
            <tr><td>Outstanding (pending)</td><td class="text-right"><strong>R<?= number_format((float)$revenue['outstanding'], 2) ?></strong></td></tr>
        </table>
        <p class="muted text-sm">Note: payments are recorded by the in-app demo flow until a live gateway is connected.</p>
    </div>

    <div class="card">
        <h2>Android downloads</h2>
        <table class="table">
            <?php
                report_row('Download requests', $apk['total']);
                report_row('Last 7 days', $apk['last_week']);
                report_row('Shared from Android', $apk['shared_from_app']);
            ?>
        </table>
        <p class="muted text-sm">Counts are requests through this website, including redirects to GitHub. They do not prove that the APK finished downloading or was installed; direct GitHub downloads are not included.</p>
    </div>
</div>
<?php require __DIR__ . '/../includes/footer.php'; ?>
