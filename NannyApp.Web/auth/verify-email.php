<?php
/**
 * Email Verification: Verify account via token
 * User clicks link in email to activate their account
 */
declare(strict_types=1);
require_once __DIR__ . '/../config/config.php';

// If already logged in and verified, go to dashboard
if (is_logged_in()) {
    $user = current_user();
    if ($user['email_verified']) {
        redirect(dashboard_path(user_role()));
    }
}

$token = trim($_GET['token'] ?? '');
$success = false;
$error = '';

if ($token && !auth_rate_limited('verify', $token, 5)) {
    $success = consume_verification_token($token);
}
if (!$success) $error = 'Invalid or expired verification link. Please request a new link.';

$pageTitle = 'Email Verification';
require __DIR__ . '/../includes/header.php';
?>
<div class="auth">
    <div class="auth-main">
        <p class="h-eyebrow">Email Verification</p>
        
        <?php if ($success): ?>
            <h1>✓ Email Verified!</h1>
            <p class="muted auth-footnote">Your email has been successfully verified. Welcome to <?php echo e(APP_NAME); ?>!</p>
            <a class="btn btn-primary auth-action-gap btn-inline" href="<?php echo url('auth/login.php'); ?>">Log in →</a>
        <?php else: ?>
            <h1>Email Verification Failed</h1>
            <div class="flash flash-error auth-footnote">
                <?php echo e($error); ?>
            </div>
            <p class="muted auth-action-gap">
                <a href="<?php echo url('auth/register.php'); ?>">← Create a new account</a> | 
                <a href="<?php echo url('auth/login.php'); ?>">Log in</a>
            </p>
        <?php endif; ?>
    </div>

    <aside class="auth-aside">
        <span class="pill">✓ Account Security</span>
        <h2>Email verified.</h2>
        <ul class="auth-list">
            <li><span class="ck">✓</span> Your account is now active</li>
            <li><span class="ck">✓</span> You can start using the platform</li>
            <li><span class="ck">✓</span> Keep your email private</li>
        </ul>
    </aside>
</div>
<?php require __DIR__ . '/../includes/footer.php'; ?>
