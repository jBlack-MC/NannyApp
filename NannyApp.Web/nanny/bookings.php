<?php
require_once __DIR__ . '/../config/config.php';
require_role('nanny');

auto_release_stale_payments();

$me = current_user()['id'];

// Accept / reject / check-in / check-out / cancel a booking.
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
    redirect('nanny/bookings.php');
}

$stmt = db()->prepare(
    "SELECT b.*, u.full_name AS parent_name, pay.amount, pay.status AS pay_status, pay.payout_status
     FROM bookings b
     JOIN users u ON u.id = b.parent_id
     LEFT JOIN payments pay ON pay.booking_id = b.id
     WHERE b.nanny_id = ? ORDER BY FIELD(b.status,'pending','confirmed','in_progress','completed','disputed','rejected','cancelled'), b.date_time DESC"
);
$stmt->execute([$me]);
$rows = $stmt->fetchAll();

$pageTitle = 'Booking requests';
require __DIR__ . '/../includes/header.php';
?>
<h1>Booking requests</h1>
<div class="card card-note-info">
    <h3>🔒 How payment protection works</h3>
    <p class="muted">When you accept a job, the parent's payment is charged and held securely — it is not yours yet. The parent gets a one-time PIN to hand you in person once you arrive. Enter it to check in, then check out when the session ends. Payment is released to you once the parent confirms (or automatically after 48 hours if they don't respond).</p>
</div>
<div class="card section">
    <?php if (!$rows): ?>
        <div class="empty">
            <span class="empty-ico">📭</span>
            <h3>No booking requests yet</h3>
            <p>New requests from parents will appear here. Make sure your profile is complete and verified to get discovered.</p>
            <a class="btn btn-primary" href="<?= url('nanny/profile.php') ?>">Edit my profile</a>
        </div>
    <?php else: ?>
        <table class="table">
            <thead><tr><th>Parent</th><th>When</th><th>Hours</th><th>Location</th><th>Pay</th><th>Status</th><th>Action</th></tr></thead>
            <tbody>
            <?php foreach ($rows as $r): ?>
                <tr>
                    <td>
                        <a href="<?= url('profile.php?id=' . (int)$r['parent_id']) ?>" class="pfp-link"><?= e($r['parent_name']) ?></a>
                        <a class="pfp-msg-ico" href="<?= url('messages.php?with=' . (int)$r['parent_id']) ?>" title="Message">💬</a>
                    </td>
                    <td><?= e(date('D d M Y, H:i', strtotime($r['date_time']))) ?>
                        <?= render_booking_timeline($r) ?>
                    </td>
                    <td><?= e(rtrim(rtrim((string)$r['duration'], '0'), '.')) ?></td>
                    <td><?= e($r['location']) ?></td>
                    <td>R<?= number_format((float)$r['amount'], 2) ?></td>
                    <td><?= status_badge($r['status']) ?></td>
                    <td>
                        <?php if ($r['status'] === 'pending'): ?>
                            <form method="post" class="inline-form">
                                <?= csrf_field() ?>
                                <input type="hidden" name="booking_id" value="<?= (int)$r['id'] ?>">
                                <button class="btn btn-sm btn-primary" name="action" value="accept">Accept</button>
                                <button class="btn btn-sm btn-danger" name="action" value="reject" data-confirm="Reject this request?">Reject</button>
                            </form>
                        <?php elseif ($r['status'] === 'confirmed'): ?>
                            <form method="post" class="checkin-form">
                                <?= csrf_field() ?>
                                <input type="hidden" name="booking_id" value="<?= (int)$r['id'] ?>">
                                <input type="hidden" name="action" value="check_in">
                                <input type="text" name="check_in_code" inputmode="numeric" pattern="[0-9]{6}" maxlength="6"
                                       placeholder="PIN from parent" class="checkin-input" required>
                                <button class="btn btn-sm btn-primary">Check in</button>
                            </form>
                            <?php if (strtotime($r['date_time']) > time()): ?>
                            <form method="post" class="inline-form">
                                <?= csrf_field() ?>
                                <input type="hidden" name="booking_id" value="<?= (int)$r['id'] ?>">
                                <button class="btn btn-sm btn-danger" name="action" value="cancel" data-confirm="Cancel this booking? The parent will be notified.">Cancel</button>
                            </form>
                            <?php endif; ?>
                        <?php elseif ($r['status'] === 'in_progress' && !$r['checked_out_at']): ?>
                            <form method="post" class="inline-form">
                                <?= csrf_field() ?>
                                <input type="hidden" name="booking_id" value="<?= (int)$r['id'] ?>">
                                <button class="btn btn-sm" name="action" value="check_out" data-confirm="Mark this session as finished?">Finish session</button>
                            </form>
                        <?php elseif ($r['status'] === 'in_progress' && $r['checked_out_at']): ?>
                            <span class="muted">Awaiting parent confirmation</span>
                        <?php elseif ($r['status'] === 'disputed'): ?>
                            <span class="muted">Under review by support</span>
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
