<?php
declare(strict_types=1);

/** Atomically reserve an attempt. All instances must use the same database. */
function is_rate_limited(string $key, int $maxAttempts = 5, int $windowSeconds = 300): bool
{
    $pdo = db();
    try {
        $pdo->beginTransaction();
        $key = hash('sha256', $key);
        // Upsert takes an exclusive row lock, avoiding INSERT IGNORE's shared-lock upgrade race.
        $pdo->prepare('INSERT INTO security_rate_limits (bucket, attempts, resets_at) VALUES (?, 0, 0) ON DUPLICATE KEY UPDATE bucket = VALUES(bucket)')->execute([$key]);
        $stmt = $pdo->prepare('SELECT attempts, resets_at, UNIX_TIMESTAMP() AS now_at FROM security_rate_limits WHERE bucket = ? FOR UPDATE');
        $stmt->execute([$key]);
        $row = $stmt->fetch();
        $now = (int) $row['now_at'];
        $attempts = (int) $row['resets_at'] <= $now ? 0 : (int) $row['attempts'];
        $reset = $attempts === 0 ? $now + $windowSeconds : (int) $row['resets_at'];
        $blocked = $attempts >= $maxAttempts;
        if (!$blocked) {
            $pdo->prepare('UPDATE security_rate_limits SET attempts = ?, resets_at = ? WHERE bucket = ?')->execute([$attempts + 1, $reset, $key]);
        }
        $pdo->commit();
        return $blocked;
    } catch (Throwable $e) {
        if ($pdo->inTransaction()) $pdo->rollBack();
        error_log('Security rate limiter unavailable');
        return true; // Never fail open when the security migration is missing.
    }
}

function auth_rate_limited(string $action, string $account, int $limit = 5): bool
{
    $ipLimited = is_rate_limited($action . ':ip:' . ($_SERVER['REMOTE_ADDR'] ?? 'unknown'), $limit * 4, 900);
    $accountLimited = is_rate_limited($action . ':account:' . strtolower(trim($account)), $limit, 900);
    return $ipLimited || $accountLimited;
}

function verified_active_user(?array $user): bool
{
    return $user !== null && (int) ($user['email_verified'] ?? 0) === 1 && ($user['status'] ?? '') === 'active';
}

function consume_password_reset(string $token, string $password): bool
{
    if (strlen($password) < 8 || !preg_match('/^[a-f0-9]{64}$/', $token)) return false;
    $pdo = db();
    $pdo->beginTransaction();
    try {
        // Lock the user first so different reset tokens for one account serialize.
        $stmt = $pdo->prepare('SELECT u.id FROM users u JOIN password_resets r ON r.user_id = u.id WHERE r.token = ? LIMIT 1');
        $stmt->execute([$token]);
        $uid = $stmt->fetchColumn();
        if (!$uid) { $pdo->rollBack(); return false; }
        $pdo->prepare('SELECT id FROM users WHERE id = ? FOR UPDATE')->execute([$uid]);
        $stmt = $pdo->prepare('SELECT id FROM password_resets WHERE user_id = ? AND token = ? AND used = 0 AND expires_at > NOW() FOR UPDATE');
        $stmt->execute([$uid, $token]);
        if (!$stmt->fetch()) { $pdo->rollBack(); return false; }
        $pdo->prepare('UPDATE users SET password_hash = ?, remember_token = NULL WHERE id = ?')->execute([password_hash($password, PASSWORD_DEFAULT), $uid]);
        $pdo->prepare('UPDATE password_resets SET used = 1 WHERE user_id = ?')->execute([$uid]);
        $pdo->prepare('DELETE FROM api_tokens WHERE user_id = ?')->execute([$uid]);
        $pdo->commit();
        return true;
    } catch (Throwable $e) {
        if ($pdo->inTransaction()) $pdo->rollBack();
        throw $e;
    }
}

function consume_verification_token(string $token): bool
{
    if (!preg_match('/^[a-f0-9]{64}$/', $token)) return false;
    $stmt = db()->prepare('UPDATE users SET email_verified = 1, verification_token = NULL, verification_sent_at = NULL WHERE verification_token = ? AND email_verified = 0 AND verification_sent_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR)');
    $stmt->execute([$token]);
    return $stmt->rowCount() === 1;
}

/** Unknown paths are private. Portfolio ownership takes precedence over public references. */
function media_access_allowed(string $path, ?array $user): bool
{
    if ($path === '' || str_contains($path, '..') || str_contains($path, "\0") || str_contains($path, '\\') || str_starts_with($path, '/') || str_contains($path, ':')) return false;
    $stmt = db()->prepare('SELECT nanny_id FROM nanny_portfolio WHERE file_path = ?');
    $stmt->execute([$path]);
    $owners = $stmt->fetchAll(PDO::FETCH_COLUMN);
    if ($owners) {
        return verified_active_user($user) && ($user['role'] === 'admin' || in_array((int) $user['id'], array_map('intval', $owners), true));
    }
    if (str_starts_with($path, 'portfolio/') || str_contains($path, '/docs/')) return false;
    $stmt = db()->prepare('SELECT id FROM users WHERE profile_image = ? UNION ALL SELECT user_id FROM nanny_profiles WHERE photo_url = ? OR banner_image = ? LIMIT 1');
    $stmt->execute([$path, $path, $path]);
    return (bool) $stmt->fetchColumn();
}
