<?php
declare(strict_types=1);

/** Explicit transport: PHP mail configured by the host; no recipient/body/token logs. */
function send_email(string $to, string $subject, string $body, string $htmlBody = ''): bool
{
    $from = getenv('NANNYAPP_MAIL_FROM') ?: '';
    if (!filter_var($to, FILTER_VALIDATE_EMAIL) || !filter_var($from, FILTER_VALIDATE_EMAIL) || preg_match('/[\r\n]/', $to . $from . $subject)) return false;
    if ((getenv('NANNYAPP_MAIL_TRANSPORT') ?: 'disabled') !== 'mail') return false;
    return @mail($to, $subject, $body, "From: $from\r\nContent-Type: text/plain; charset=UTF-8");
}

function account_email_link(string $path, string $token): string
{
    $base = rtrim(getenv('NANNYAPP_BASE_URL') ?: '', '/');
    $scheme = parse_url($base, PHP_URL_SCHEME);
    $host = parse_url($base, PHP_URL_HOST);
    if (!$host || ($scheme !== 'https' && !($scheme === 'http' && in_array($host, ['localhost', '127.0.0.1'], true)))) {
        throw new RuntimeException('Configure an absolute NANNYAPP_BASE_URL for account email links.');
    }
    return $base . '/auth/' . $path . '?token=' . rawurlencode($token);
}

function send_verification_email(int $userId, string $email, string $name): bool
{
    if (getenv('NANNYAPP_MAIL_TRANSPORT') === 'resend') {
        require_once __DIR__ . '/email_outbox.php';
        return queue_account_email($userId, $email, 'verify');
    }
    $token = bin2hex(random_bytes(32));
    try {
        $link = account_email_link('verify-email.php', $token);
        db()->prepare('UPDATE users SET verification_token = ?, verification_sent_at = NOW() WHERE id = ? AND email_verified = 0')->execute([$token, $userId]);
        $sent = send_email($email, 'Verify your Nanny-App email', "Please verify your email: $link\nThis link expires in 24 hours.");
        if (!$sent) db()->prepare('UPDATE users SET verification_token = NULL, verification_sent_at = NULL WHERE id = ? AND verification_token = ?')->execute([$userId, $token]);
        return $sent;
    } catch (Throwable) {
        error_log('Verification email delivery failed');
        return false;
    }
}

function send_password_reset_email(int $userId, string $email, string $name): bool
{
    if (getenv('NANNYAPP_MAIL_TRANSPORT') === 'resend') {
        require_once __DIR__ . '/email_outbox.php';
        return queue_account_email($userId, $email, 'reset');
    }
    $token = bin2hex(random_bytes(32));
    try {
        $link = account_email_link('reset.php', $token);
        db()->prepare('INSERT INTO password_resets (user_id, token, expires_at) VALUES (?, ?, DATE_ADD(NOW(), INTERVAL 1 HOUR))')->execute([$userId, $token]);
        $sent = send_email($email, 'Reset your Nanny-App password', "Reset your password: $link\nThis link expires in one hour.");
        if (!$sent) db()->prepare('DELETE FROM password_resets WHERE user_id = ? AND token = ?')->execute([$userId, $token]);
        return $sent;
    } catch (Throwable) {
        error_log('Password reset email delivery failed');
        return false;
    }
}
