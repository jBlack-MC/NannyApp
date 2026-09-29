<?php
require_once __DIR__ . '/../config/config.php';
require_role('parent');

auto_release_stale_payments();

$me = current_user()['id'];

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    verify_csrf();
    require_once __DIR__ . '/../../NannyApp.Shared/config/bookings.php';
    try {
        $action=$_POST['action']??'';
        if (!is_string($action)) throw new BookingError('Invalid action.',400);
        transition_booking(db(),(int)$me,(int)($_POST['booking_id']??0),$action,[
            'checkInCode'=>$_POST['check_in_code']??null,'reason'=>$_POST['dispute_reason']??null
        ]);
        flash('Booking updated. Payment entries are a manual ledger; no bank transfer was made.');
    } catch (BookingError $e) { flash($e->getMessage(),'error'); }
    catch (Throwable $e) { flash('Could not update booking. Please retry.','error'); }
    redirect('parent/bookings.php');
}

$stmt = db()->prepare(
    "SELECT b.*, u.full_name AS nanny_name, pay.amount, pay.status AS pay_status, pay.payout_status,
            (SELECT COUNT(*) FROM reviews r WHERE r.booking_id = b.id) AS reviewed
     FROM bookings b
     JOIN users u ON u.id = b.nanny_id
     LEFT JOIN payments pay ON pay.booking_id = b.id
     WHERE b.parent_id = ? ORDER BY FIELD(b.status,'pending','confirmed','in_progress','completed','disputed','rejected','cancelled'), b.date_time DESC"
);
$stmt->execute([$me]);
$rows = $stmt->fetchAll();

$pageTitle = 'My bookings';
require __DIR__ . '/../includes/header.php';
?>
<h1>My bookings</h1>
<div class="card section">
    <?php if (!$rows): ?>
        <div class="empty">
            <span class="empty-ico">🗓️</span>
            <h3>No bookings yet</h3>
            <p>When you book a nanny, your sessions and their status will show up here.</p>
            <a class="btn btn-primary" href="<?= url('parent/nannies.php') ?>">Find a nanny</a>
        </div>
    <?php else: ?>
        <table class="table">
            <thead><tr><th>Nanny</th><th>When</th><th>Hours</th><th>Est. cost</th><th>Status</th><th></th></tr></thead>
            <tbody>
            <?php foreach ($rows as $r): ?>
                <tr>
                    <td><a href="<?= url('messages.php?with=' . (int)$r['nanny_id']) ?>"><?= e($r['nanny_name']) ?></a></td>
                    <td><?= e(date('D d M Y, H:i', strtotime($r['date_time']))) ?>
                        <?= render_booking_timeline($r) ?>
                    </td>
                    <td><?= e(rtrim(rtrim((string)$r['duration'], '0'), '.')) ?></td>
                    <td>R<?= number_format((float)$r['amount'], 2) ?></td>
                    <td><?= status_badge($r['status']) ?></td>
                    <td>
                        <?php if ($r['status'] === 'pending'): ?>
                            <form method="post" class="form-zero">
                                <?= csrf_field() ?>
                                <input type="hidden" name="booking_id" value="<?= (int)$r['id'] ?>">
                                <button class="btn btn-sm btn-danger" name="action" value="cancel" data-confirm="Cancel this booking?">Cancel</button>
                            </form>

                        <?php elseif ($r['status'] === 'confirmed'): ?>
                            <div class="checkin-pin-box">
                                <span class="muted text-xs">Check-in PIN — give this to <?= e(explode(' ', $r['nanny_name'])[0]) ?> only once they've arrived:</span>
                                <strong class="checkin-pin"><?= e($r['check_in_code'] ?? '——————') ?></strong>
                            </div>
                            <div class="booking-actions-row">
                                <form method="post" class="form-zero">
                                    <?= csrf_field() ?>
                                    <input type="hidden" name="booking_id" value="<?= (int)$r['id'] ?>">
                                    <button class="btn btn-sm btn-danger" name="action" value="cancel" data-confirm="Cancel this booking?">Cancel</button>
                                </form>
                                <form method="post" class="form-zero">
                                    <?= csrf_field() ?>
                                    <input type="hidden" name="booking_id" value="<?= (int)$r['id'] ?>">
                                    <button class="btn btn-sm" name="action" value="resend_code" data-confirm="Generate a new PIN? The old one will stop working.">New PIN</button>
                                </form>
                            </div>

                        <?php elseif ($r['status'] === 'in_progress' && $r['checked_out_at']): ?>
                            <form method="post" class="form-zero mb-4">
                                <?= csrf_field() ?>
                                <input type="hidden" name="booking_id" value="<?= (int)$r['id'] ?>">
                                <button class="btn btn-sm btn-primary" name="action" value="confirm" data-confirm="Confirm the session happened and release payment to the nanny?">✓ Confirm &amp; release payment</button>
                            </form>
                            <details class="dispute-details">
                                <summary class="muted text-xs">Something wrong?</summary>
                                <form method="post" class="stack-tight mt-4">
                                    <?= csrf_field() ?>
                                    <input type="hidden" name="booking_id" value="<?= (int)$r['id'] ?>">
                                    <input type="hidden" name="action" value="dispute">
                                    <textarea name="dispute_reason" rows="2" class="text-xs" placeholder="What happened?" required></textarea>
                                    <button class="btn btn-sm btn-danger" data-confirm="Report a problem with this booking? Payment will be held for review.">Report a problem</button>
                                </form>
                            </details>

                        <?php elseif ($r['status'] === 'in_progress'): ?>
                            <span class="muted">Session in progress</span><br>
                            <details class="dispute-details">
                                <summary class="muted text-xs">Nanny never arrived?</summary>
                                <form method="post" class="stack-tight mt-4">
                                    <?= csrf_field() ?>
                                    <input type="hidden" name="booking_id" value="<?= (int)$r['id'] ?>">
                                    <input type="hidden" name="action" value="dispute">
                                    <textarea name="dispute_reason" rows="2" class="text-xs" placeholder="What happened?" required></textarea>
                                    <button class="btn btn-sm btn-danger" data-confirm="Report a problem with this booking? Payment will be held for review.">Report a problem</button>
                                </form>
                            </details>

                        <?php elseif ($r['status'] === 'completed' && !(int)$r['reviewed']): ?>
                            <a class="btn btn-sm btn-primary" href="<?= url('parent/review.php?booking=' . (int)$r['id']) ?>">Leave review</a>
                        <?php elseif ($r['status'] === 'completed' && (int)$r['reviewed']): ?>
                            <span class="muted">Reviewed ✓</span>
                        <?php elseif ($r['status'] === 'disputed'): ?>
                            <span class="muted">Under review by support</span>
                        <?php endif; ?>
                    </td>
                </tr>
            <?php endforeach; ?>
            </tbody>
        </table>
    <?php endif; ?>
</div>
<?php require __DIR__ . '/../includes/footer.php'; ?>
