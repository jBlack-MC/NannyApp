<?php
/** Report a parent or nanny to the safety team. Linked from profiles: report.php?user=ID[&booking=ID] */
require_once __DIR__ . '/config/config.php';
require_once __DIR__ . '/../NannyApp.Shared/config/reports.php';
require_role('parent', 'nanny');

$me = current_user();
$reportedId = (int) ($_GET['user'] ?? $_POST['user'] ?? 0);
$bookingId = (int) ($_GET['booking'] ?? $_POST['booking'] ?? 0) ?: null;
$stmt = db()->prepare("SELECT id, full_name, role FROM users WHERE id = ? AND role IN ('parent','nanny')");
$stmt->execute([$reportedId]);
$reported = $stmt->fetch();
if (!$reported || (int) $reported['id'] === (int) $me['id']) {
    flash('That account could not be found.', 'error');
    redirect('support.php');
}

$reasons = report_reasons();
$errors = [];
$done = false;
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    verify_csrf();
    if (is_rate_limited('report:' . $me['id'], 5, 3600)) {
        $errors[] = 'You have sent several reports recently. If anyone is in danger, call 10111 now.';
    } else {
        try {
            create_user_report(db(), $me, (int) $reported['id'], $bookingId, (string) ($_POST['reason'] ?? ''), (string) ($_POST['details'] ?? ''));
            $done = true;
        } catch (InvalidArgumentException $e) {
            $errors[] = $e->getMessage();
        } catch (Throwable $e) {
            log_exception($e, 'report_create_failed');
            $errors[] = 'Could not send your report. Please try again. Reference: ' . request_ref();
        }
    }
}

$pageTitle = 'Report a concern';
require __DIR__ . '/includes/header.php';
?>
<div class="page-head">
    <p class="h-eyebrow">Safety</p>
    <h1>Report a concern</h1>
    <p class="muted">Tell our safety team about <?= e($reported['full_name']) ?>. Reports are confidential: the person you report is not told who made it.</p>
</div>

<div class="grid grid-2 support-grid">
    <div>
        <?php foreach ($errors as $err): ?>
            <div class="flash flash-error"><?= e($err) ?></div>
        <?php endforeach; ?>

        <?php if ($done): ?>
            <div class="card stack support-success">
                <div class="emoji-xl"><?= icon('shield-check') ?></div>
                <h2 class="tight-heading">Report received</h2>
                <p class="muted">Thank you. Safety reports go to the top of our team's queue. We may contact you for more detail.</p>
                <a class="btn btn-primary btn-align-self-center" href="<?= url('index.php') ?>">Back to home</a>
            </div>
        <?php else: ?>
            <form method="post" class="card stack">
                <?= csrf_field() ?>
                <input type="hidden" name="user" value="<?= (int) $reported['id'] ?>">
                <?php if ($bookingId): ?><input type="hidden" name="booking" value="<?= (int) $bookingId ?>"><?php endif; ?>

                <div class="field">
                    <label for="r-reason">What is the report about?</label>
                    <select id="r-reason" name="reason" required>
                        <option value="">Choose one</option>
                        <?php foreach ($reasons as $code => $label): ?>
                            <option value="<?= e($code) ?>" <?= ($_POST['reason'] ?? '') === $code ? 'selected' : '' ?>><?= e($label) ?></option>
                        <?php endforeach; ?>
                    </select>
                </div>
                <div class="field">
                    <label for="r-details">What happened?</label>
                    <textarea id="r-details" name="details" rows="7" minlength="10" maxlength="2000" required
                              placeholder="Include dates, times and anything you saw or were told."><?= e($_POST['details'] ?? '') ?></textarea>
                </div>
                <button class="btn btn-primary btn-align-self-start">Send report</button>
            </form>
        <?php endif; ?>
    </div>

    <div class="stack">
        <div class="card stack">
            <h3 class="tight-heading">If a child is in danger now</h3>
            <p class="muted muted-tight">Do not wait for us. Call the police first, then report here.</p>
            <a class="btn btn-emergency" href="tel:10111"><?= icon('siren') ?> SAPS emergency: 10111</a>
            <a class="btn" href="tel:112"><?= icon('phone') ?> From any mobile: 112</a>
            <a class="btn" href="tel:116"><?= icon('phone') ?> Childline South Africa: 116</a>
        </div>
        <div class="card stack">
            <h3 class="tight-heading">What happens next</h3>
            <p class="muted muted-tight">An administrator reviews every safety report, can suspend accounts while they investigate, and keeps a record for future reports about the same person.</p>
        </div>
    </div>
</div>
<?php require __DIR__ . '/includes/footer.php'; ?>
