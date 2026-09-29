<?php
require_once __DIR__ . '/../config/config.php';
require_role('admin');

auto_release_stale_payments();

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    verify_csrf();
    require_once __DIR__ . '/../../NannyApp.Shared/config/bookings.php';
    try {
        $action=$_POST['action']??'';
        if (!in_array($action,['record_received','release','refund'],true)) throw new BookingError('Invalid action.',400);
        transition_booking(db(),(int)current_user()['id'],(int)($_POST['booking_id']??0),$action);
        flash('Manual ledger updated. No bank transfer was made.');
    } catch (BookingError $e) { flash($e->getMessage(),'error'); }
    catch (Throwable $e) { flash('Could not update ledger. Please retry.','error'); }
    redirect('admin/payments.php');
}

$rows = db()->query(
    "SELECT pay.id, pay.amount, pay.method, pay.transaction_id, pay.status, pay.payout_status, pay.created_at,
            pay.booking_id, b.status AS booking_status, b.dispute_reason,
            p.full_name AS parent_name, n.full_name AS nanny_name
     FROM payments pay
     JOIN bookings b ON b.id = pay.booking_id
     JOIN users p ON p.id = b.parent_id
     JOIN users n ON n.id = b.nanny_id
     ORDER BY (b.status='disputed') DESC, pay.created_at DESC"
)->fetchAll();

$totals = db()->query(
    "SELECT
        IFNULL(SUM(CASE WHEN status='paid' THEN amount END),0) AS paid,
        IFNULL(SUM(CASE WHEN payout_status='held' THEN amount END),0) AS held,
        IFNULL(SUM(CASE WHEN payout_status='released' THEN amount END),0) AS released,
        IFNULL(SUM(CASE WHEN status='pending' THEN amount END),0) AS pending
     FROM payments"
)->fetch();

$disputedCount = 0;
foreach ($rows as $r) {
    if ($r['booking_status'] === 'disputed') $disputedCount++;
}

$pageTitle = 'Payments';
require __DIR__ . '/../includes/header.php';
?>
<h1>Payments</h1>

<?php if ($disputedCount > 0): ?>
<div class="card card-note-info section">
    <h3>⚠️ <?= $disputedCount ?> disputed booking<?= $disputedCount===1?'':'s' ?> need review</h3>
    <p class="muted">A parent reported a problem — likely the nanny never arrived. Payment is on hold. Review each case below and either release the funds to the nanny or refund the parent.</p>
</div>
<?php endif; ?>

<div class="stats section">
    <div class="stat"><b>R<?= number_format((float)$totals['paid'], 0) ?></b>Total charged</div>
    <div class="stat"><b>R<?= number_format((float)$totals['held'], 0) ?></b>Held in escrow</div>
    <div class="stat"><b>R<?= number_format((float)$totals['released'], 0) ?></b>Released to nannies</div>
    <div class="stat"><b>R<?= number_format((float)$totals['pending'], 0) ?></b>Awaiting charge</div>
</div>

<div class="card section">
    <?php if (!$rows): ?>
        <p class="muted">No payments yet.</p>
    <?php else: ?>
        <table class="table">
            <thead><tr><th>#</th><th>Booking</th><th>Parent</th><th>Nanny</th><th>Amount</th><th>Charge</th><th>Payout</th><th>Booking status</th><th>Date</th><th></th></tr></thead>
            <tbody>
            <?php foreach ($rows as $r): ?>
                <tr>
                    <td class="muted">#<?= (int)$r['id'] ?></td>
                    <td class="muted">#<?= (int)$r['booking_id'] ?></td>
                    <td><?= e($r['parent_name']) ?></td>
                    <td><?= e($r['nanny_name']) ?></td>
                    <td>R<?= number_format((float)$r['amount'], 2) ?></td>
                    <td><?= status_badge($r['status']) ?><div class="muted text-xs"><?= e(ucfirst($r['method'] ?? 'manual')) ?></div></td>
                    <td><?= $r['payout_status'] ? status_badge($r['payout_status']) : '<span class="muted">—</span>' ?></td>
                    <td>
                        <?= status_badge($r['booking_status']) ?>
                        <?php if ($r['booking_status'] === 'disputed' && $r['dispute_reason']): ?>
                            <div class="muted text-xs mt-4"><?= e($r['dispute_reason']) ?></div>
                        <?php endif; ?>
                    </td>
                    <td class="muted"><?= e(date('d M Y', strtotime($r['created_at']))) ?></td>
                    <td>
                        <?php if ($r['status'] === 'pending' && $r['method'] === 'manual' && in_array($r['booking_status'], ['confirmed','in_progress','disputed','completed'], true)): ?>
                            <form method="post" class="form-zero">
                                <?= csrf_field() ?>
                                <input type="hidden" name="booking_id" value="<?= (int)$r['booking_id'] ?>">
                                <button class="btn btn-sm btn-primary" name="action" value="record_received" data-confirm="Confirm that the manual payment has been received and place it on hold?">Record payment</button>
                            </form>
                        <?php elseif (in_array($r['booking_status'], ['in_progress','disputed'], true) && $r['payout_status'] === 'held'): ?>
                            <div class="booking-actions-row">
                                <form method="post" class="form-zero">
                                    <?= csrf_field() ?>
                                    <input type="hidden" name="booking_id" value="<?= (int)$r['booking_id'] ?>">
                                    <button class="btn btn-sm btn-primary" name="action" value="release" data-confirm="Release this payment to the nanny?">Release</button>
                                </form>
                                <form method="post" class="form-zero">
                                    <?= csrf_field() ?>
                                    <input type="hidden" name="booking_id" value="<?= (int)$r['booking_id'] ?>">
                                    <button class="btn btn-sm btn-danger" name="action" value="refund" data-confirm="Refund this payment to the parent? The nanny will get nothing for this job.">Refund</button>
                                </form>
                            </div>
                        <?php else: ?>
                            <span class="muted">—</span>
                        <?php endif; ?>
                    </td>
                </tr>
            <?php endforeach; ?>
            </tbody>
        </table>
    <?php endif; ?>
</div>
<?php require __DIR__ . '/../includes/footer.php'; ?>
