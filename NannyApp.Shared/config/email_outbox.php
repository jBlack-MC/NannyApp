<?php
declare(strict_types=1);
require_once __DIR__ . '/resend.php';

/** Store the account token and immutable message in one transaction. No external IO. */
function queue_account_email(int $userId, string $email, string $purpose): bool
{
    $from = getenv('NANNYAPP_MAIL_FROM') ?: '';
    if ($userId <= 0 || !filter_var($email, FILTER_VALIDATE_EMAIL) || !filter_var($from, FILTER_VALIDATE_EMAIL)
        || preg_match('/[\r\n]/', $email . $from) || !in_array($purpose, ['verify', 'reset'], true)) return false;
    $pdo = db();
    if ($pdo->inTransaction()) throw new LogicException('Queue account mail after registration commits.');
    try {
        $token = bin2hex(random_bytes(32));
        $path = $purpose === 'verify' ? 'verify-email.php' : 'reset.php';
        $link = account_email_link($path, $token);
        $payload = json_encode([
            'from' => $from, 'to' => [$email],
            'subject' => $purpose === 'verify' ? 'Verify your Nanny-App email' : 'Reset your Nanny-App password',
            'text' => ($purpose === 'verify' ? 'Verify your email: ' : 'Reset your password: ') . $link,
        ], JSON_THROW_ON_ERROR | JSON_UNESCAPED_SLASHES);
        $pdo->beginTransaction();
        $stmt = $pdo->prepare('SELECT email, email_verified FROM users WHERE id = ? FOR UPDATE');
        $stmt->execute([$userId]);
        $user = $stmt->fetch();
        if (!$user || strcasecmp($user['email'], $email) !== 0 || ($purpose === 'verify' && (int) $user['email_verified'] === 1)) {
            $pdo->rollBack();
            return false;
        }
        if ($purpose === 'verify') {
            $pdo->prepare('UPDATE users SET verification_token=?, verification_sent_at=NOW() WHERE id=?')->execute([$token, $userId]);
        } else {
            $pdo->prepare('INSERT INTO password_resets (user_id,token,expires_at) VALUES (?,?,DATE_ADD(NOW(),INTERVAL 1 HOUR))')->execute([$userId, $token]);
        }
        // Stop well before the provider's 24-hour deduplication window expires.
        $ttl = $purpose === 'verify' ? 82800 : 3500;
        $pdo->prepare('INSERT INTO email_outbox (id,user_id,purpose,token_hash,payload,available_at,expires_at) VALUES (?,?,?,?,?,NOW(),DATE_ADD(NOW(),INTERVAL ? SECOND))')
            ->execute([bin2hex(random_bytes(16)), $userId, $purpose, hash('sha256', $token), $payload, $ttl]);
        $pdo->commit();
        return true; // Queued, not proof of inbox delivery.
    } catch (Throwable) {
        if ($pdo->inTransaction()) $pdo->rollBack();
        error_log('mail_queue_failed');
        return false;
    }
}

function claim_account_email(PDO $pdo): ?array
{
    $pdo->beginTransaction();
    try {
        $pdo->exec("UPDATE email_outbox SET status='expired',payload=NULL,lease_token=NULL,lease_until=NULL WHERE status IN ('pending','sending') AND expires_at <= NOW()");
        $stmt = $pdo->query("SELECT * FROM email_outbox WHERE expires_at > NOW() AND ((status='pending' AND available_at <= NOW()) OR (status='sending' AND lease_until < NOW())) ORDER BY available_at,id LIMIT 1 FOR UPDATE");
        $job = $stmt->fetch();
        if (!$job) { $pdo->commit(); return null; }
        if ((int) $job['attempts'] >= 8) {
            $pdo->prepare("UPDATE email_outbox SET status='failed',payload=NULL,last_code='retry_exhausted',lease_token=NULL,lease_until=NULL WHERE id=?")->execute([$job['id']]);
            $pdo->commit();
            return null;
        }
        $job['lease_token'] = bin2hex(random_bytes(16));
        $job['attempts'] = (int) $job['attempts'] + 1;
        $pdo->prepare("UPDATE email_outbox SET status='sending',attempts=?,lease_token=?,lease_until=DATE_ADD(NOW(),INTERVAL 120 SECOND) WHERE id=?")
            ->execute([$job['attempts'], $job['lease_token'], $job['id']]);
        $pdo->commit();
        return $job;
    } catch (Throwable $e) {
        if ($pdo->inTransaction()) $pdo->rollBack();
        throw $e;
    }
}

function deliver_account_email(PDO $pdo, array $job, ?callable $http = null): string
{
    $lease = $pdo->prepare("SELECT id FROM email_outbox WHERE id=? AND lease_token=? AND status='sending' AND lease_until > NOW() AND expires_at > NOW()");
    $lease->execute([$job['id'], $job['lease_token']]);
    if (!$lease->fetch()) return 'stale';
    // Resends and password recovery can invalidate queued links before delivery.
    $sql = $job['purpose'] === 'verify'
        ? 'SELECT id FROM users WHERE id=? AND email_verified=0 AND SHA2(verification_token,256)=? AND verification_sent_at >= DATE_SUB(NOW(),INTERVAL 24 HOUR)'
        : 'SELECT id FROM password_resets WHERE user_id=? AND used=0 AND SHA2(token,256)=? AND expires_at > NOW()';
    $stmt = $pdo->prepare($sql);
    $stmt->execute([$job['user_id'], $job['token_hash']]);
    $result = $stmt->fetch() ? resend_deliver($job, $http) : ['state'=>'expired','code'=>'superseded','delay'=>0];
    $retry = $result['state'] === 'retry' && (int) $job['attempts'] < 8;
    $status = $retry ? 'pending' : ($result['state'] === 'retry' ? 'failed' : $result['state']);
    $delay = max((int) $result['delay'], min(3600, 30 * (2 ** ((int) $job['attempts'] - 1))) + random_int(0, 15));
    // Lease fence prevents a slow/stale worker overwriting a newer worker's result.
    $stmt = $pdo->prepare('UPDATE email_outbox SET status=?,payload=?,provider_id=?,last_code=?,available_at=DATE_ADD(NOW(),INTERVAL ? SECOND),lease_token=NULL,lease_until=NULL WHERE id=? AND status=\'sending\' AND lease_token=?');
    $stmt->execute([$status, $retry ? $job['payload'] : null, $result['provider_id'] ?? null, $result['code'], $delay, $job['id'], $job['lease_token']]);
    return $stmt->rowCount() === 1 ? $status : 'stale';
}
