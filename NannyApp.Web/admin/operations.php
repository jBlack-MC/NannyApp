<?php
require_once __DIR__ . '/../config/config.php';
require_role('admin');

// Read-only triage: sensitive mutations stay in their existing CSRF-protected pages.
$counts = db()->query("SELECT
    (SELECT COUNT(*) FROM nanny_profiles WHERE verification_status='pending') AS profiles,
    (SELECT COUNT(*) FROM nanny_portfolio WHERE admin_verified=0) AS documents,
    (SELECT COUNT(*) FROM bookings WHERE status='disputed') AS disputes,
    (SELECT COUNT(*) FROM support_tickets WHERE status IN ('open','in_progress')) AS tickets,
    (SELECT COUNT(*) FROM support_tickets WHERE category='safety' AND status IN ('open','in_progress')) AS safety,
    (SELECT COUNT(*) FROM payments WHERE method='manual' AND status='pending') AS receipts,
    (SELECT COUNT(*) FROM contact_messages WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)) AS contacts")->fetch();
$queues = db()->query("SELECT
    (SELECT COUNT(*) FROM email_outbox WHERE status='pending') AS emails,
    (SELECT COUNT(*) FROM email_outbox WHERE status='failed') AS failed_emails,
    (SELECT COUNT(*) FROM notification_outbox WHERE delivered_at IS NULL) AS notifications,
    (SELECT COUNT(*) FROM storage_deletions WHERE completed_at IS NULL) AS deletions")->fetch();
require_once __DIR__ . '/../../NannyApp.Shared/config/health.php';
$health = system_health(db());
$tickets = db()->query("SELECT id, subject, category, status, created_at FROM support_tickets
    WHERE status IN ('open','in_progress')
    ORDER BY (category='safety') DESC, created_at ASC LIMIT 10")->fetchAll();
$actions = [
    ['Safety concerns', 'safety', 'admin/support.php?category=safety'],
    ['Disputed bookings', 'disputes', 'admin/bookings.php?status=disputed'],
    ['Profiles awaiting verification', 'profiles', 'admin/verifications.php'],
    ['Documents awaiting review', 'documents', 'admin/verifications.php'],
    ['Active support tickets', 'tickets', 'admin/support.php'],
    ['Pending manual receipts', 'receipts', 'admin/payments.php'],
    ['Contact messages in the last 7 days', 'contacts', 'admin/contacts.php'],
];
$pageTitle = 'Admin action centre';
require __DIR__ . '/../includes/header.php';
?>
<div class="section-head">
    <div><p class="h-eyebrow">Admin</p><h1>Action centre</h1>
        <p class="muted">Review outstanding work. Safety concerns appear first.</p></div>
    <a class="btn" href="<?= url('admin/dashboard.php') ?>">Platform overview</a>
</div>
<div class="admin-action-grid section">
    <?php foreach ($actions as [$label, $key, $path]): ?>
        <a class="card admin-action-card" href="<?= url($path) ?>">
            <strong><?= (int) $counts[$key] ?></strong><span><?= e($label) ?></span>
        </a>
    <?php endforeach; ?>
</div>
<section class="card section" aria-labelledby="support-work">
    <h2 id="support-work">Oldest active support tickets</h2>
    <?php if (!$tickets): ?><p class="muted">No active tickets.</p><?php else: ?>
    <div class="table-wrap-scroll" role="region" aria-label="Active support tickets" tabindex="0">
        <table class="table"><thead><tr><th>Ticket</th><th>Subject</th><th>Category</th><th>Status</th><th>Created</th><th>Review</th></tr></thead><tbody>
        <?php foreach ($tickets as $ticket): ?>
            <tr><td>#<?= (int) $ticket['id'] ?></td><td><?= e($ticket['subject']) ?></td>
                <td><?= e($ticket['category']) ?></td><td><?= status_badge($ticket['status']) ?></td>
                <td><?= e($ticket['created_at']) ?></td>
                <td><a href="<?= url('admin/support.php?category=' . urlencode($ticket['category']) . '&status=' . urlencode($ticket['status'])) ?>">Review tickets</a></td></tr>
        <?php endforeach; ?>
        </tbody></table>
    </div>
    <?php endif; ?>
</section>
<section class="card section" aria-labelledby="worker-status">
    <h2 id="worker-status">Delivery queues</h2>
    <p class="muted">Refresh to check progress. Growing queues may mean a worker needs attention.</p>
    <dl class="admin-queue-grid">
        <div><dt>Pending account emails</dt><dd><?= (int) $queues['emails'] ?></dd></div>
        <div><dt>Failed account emails</dt><dd><?= (int) $queues['failed_emails'] ?></dd></div>
        <div><dt>Pending notifications</dt><dd><?= (int) $queues['notifications'] ?></dd></div>
        <div><dt>Pending file deletions</dt><dd><?= (int) $queues['deletions'] ?></dd></div>
    </dl>
    <p class="muted">Queue counts do not confirm email delivery or worker health. Manual ledger updates do not transfer money.</p>
</section>
<section class="card section" aria-labelledby="system-health">
    <h2 id="system-health">System health</h2>
    <p class="muted"><?= $health['status'] === 'ok' ? 'All checks passed.' : 'One or more checks need attention.' ?>
        An uptime monitor can watch the same checks at <code>/api/health.php</code>.</p>
    <dl class="admin-queue-grid">
        <?php foreach ($health['checks'] as $name => $check): ?>
            <div><dt><?= e(ucfirst($name)) ?> <span class="badge <?= $check['ok'] ? 'badge-ok' : 'badge-bad' ?>"><?= $check['ok'] ? 'OK' : 'Needs attention' ?></span></dt>
                <dd class="admin-health-detail"><?= e($check['detail']) ?></dd></div>
        <?php endforeach; ?>
    </dl>
    <p class="muted">When a user reports an error with a reference code, search the server log for that code.</p>
</section>
<div class="dash-wrap-actions section">
    <a class="btn" href="<?= url('admin/users.php') ?>">Manage users</a>
    <a class="btn" href="<?= url('admin/notify.php') ?>">Send a notice</a>
    <a class="btn" href="<?= url('admin/reports.php') ?>">Reports</a>
    <a class="btn" href="<?= url('admin/contacts.php') ?>">Contact inbox</a>
</div>
<?php require __DIR__ . '/../includes/footer.php'; ?>
