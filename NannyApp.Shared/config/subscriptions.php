<?php
/**
 * Nanny-App Plus: one subscription sold through Google Play Billing.
 *
 * The Android app buys it; this file checks every purchase token with the Google Play
 * Developer API before granting anything, stores the result, and answers "is this account
 * subscribed?" for the API and the website. Nothing here trusts what the phone says.
 *
 * Configuration (environment):
 *   NANNYAPP_PLAY_PACKAGE               application id, default com.nannyapp
 *   NANNYAPP_PLAY_SUBSCRIPTION_ID       Play Console product id, default nannyapp_plus
 *   NANNYAPP_PLAY_SERVICE_ACCOUNT_FILE  path to the Google Cloud service-account JSON key that has
 *                                       access to the app in Play Console (Users and permissions).
 */

declare(strict_types=1);

const PLUS_FREE_SAVED_NANNIES = 3;

function plus_product_id(): string
{
    return getenv('NANNYAPP_PLAY_SUBSCRIPTION_ID') ?: 'nannyapp_plus';
}

function plus_configured(): bool
{
    $file = getenv('NANNYAPP_PLAY_SERVICE_ACCOUNT_FILE') ?: '';
    return $file !== '' && is_readable($file);
}

/** Matches the obfuscatedAccountId the app attaches to the purchase, so a token cannot be moved to another account. */
function plus_account_reference(int $userId): string
{
    return hash('sha256', 'nannyapp-account:' . $userId);
}

// Not cached: a purchase verified earlier in the same request must count straight away.
function plus_active(PDO $pdo, int $userId): bool
{
    $stmt = $pdo->prepare("SELECT 1 FROM subscriptions WHERE user_id = ? AND expires_at > NOW()
        AND status IN ('active','grace','canceled') LIMIT 1");
    $stmt->execute([$userId]);
    return (bool) $stmt->fetchColumn();
}

/** @return array{active:bool,status:?string,expiresAt:?string,productId:string,configured:bool} */
function plus_status(PDO $pdo, int $userId): array
{
    $stmt = $pdo->prepare('SELECT status, expires_at FROM subscriptions WHERE user_id = ? ORDER BY expires_at DESC LIMIT 1');
    $stmt->execute([$userId]);
    $row = $stmt->fetch(PDO::FETCH_ASSOC) ?: null;
    return [
        'active' => plus_active($pdo, $userId),
        'status' => $row['status'] ?? null,
        'expiresAt' => $row['expires_at'] ?? null,
        'productId' => plus_product_id(),
        'configured' => plus_configured(),
    ];
}

/**
 * Verifies a Play purchase token, records it and acknowledges it with Google.
 * $http(string $method, string $url, array $headers, ?string $body): array{status:int,body:string}
 *
 * @throws InvalidArgumentException with a message safe to show the user
 * @return array the new plus_status()
 */
function plus_verify_purchase(PDO $pdo, int $userId, string $productId, string $token, ?callable $http = null): array
{
    if ($productId !== plus_product_id()) throw new InvalidArgumentException('Unknown subscription.');
    if ($token === '' || strlen($token) > 512) throw new InvalidArgumentException('Invalid purchase.');
    $http ??= 'plus_http';
    $package = getenv('NANNYAPP_PLAY_PACKAGE') ?: 'com.nannyapp';
    $auth = ['Authorization: Bearer ' . plus_google_access_token($http)];

    $base = 'https://androidpublisher.googleapis.com/androidpublisher/v3/applications/' . rawurlencode($package) . '/purchases/';
    $reply = $http('GET', $base . 'subscriptionsv2/tokens/' . rawurlencode($token), $auth, null);
    if ($reply['status'] === 404 || $reply['status'] === 410) throw new InvalidArgumentException('Google Play does not recognise this purchase.');
    if ($reply['status'] !== 200) throw new RuntimeException('Play verification failed with HTTP ' . $reply['status']);
    $data = json_decode($reply['body'], true);
    if (!is_array($data)) throw new RuntimeException('Play verification returned invalid JSON');

    $owner = $data['externalAccountIdentifiers']['obfuscatedExternalAccountId'] ?? null;
    if ($owner !== null && !hash_equals(plus_account_reference($userId), (string) $owner)) {
        throw new InvalidArgumentException('This purchase belongs to a different account.');
    }
    $item = null;
    foreach ($data['lineItems'] ?? [] as $line) {
        if (($line['productId'] ?? '') === $productId) $item = $line;
    }
    if ($item === null) throw new InvalidArgumentException('This purchase is not Nanny-App Plus.');

    $status = match ($data['subscriptionState'] ?? '') {
        'SUBSCRIPTION_STATE_ACTIVE' => 'active',
        'SUBSCRIPTION_STATE_IN_GRACE_PERIOD' => 'grace',
        'SUBSCRIPTION_STATE_CANCELED' => 'canceled',
        'SUBSCRIPTION_STATE_ON_HOLD' => 'on_hold',
        'SUBSCRIPTION_STATE_PAUSED' => 'paused',
        'SUBSCRIPTION_STATE_PENDING' => 'pending',
        default => 'expired',
    };
    $expiry = isset($item['expiryTime']) ? (new DateTimeImmutable($item['expiryTime']))->setTimezone(new DateTimeZone(date_default_timezone_get()))->format('Y-m-d H:i:s') : null;
    $hash = hash('sha256', $token);

    $existing = $pdo->prepare('SELECT user_id FROM subscriptions WHERE token_hash = ?');
    $existing->execute([$hash]);
    $holder = $existing->fetchColumn();
    if ($holder !== false && (int) $holder !== $userId) throw new InvalidArgumentException('This purchase belongs to a different account.');

    $pdo->prepare('INSERT INTO subscriptions (user_id, product_id, purchase_token, token_hash, status, expires_at, auto_renewing, last_verified_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, NOW())
        ON DUPLICATE KEY UPDATE status = VALUES(status), expires_at = VALUES(expires_at), auto_renewing = VALUES(auto_renewing), last_verified_at = NOW()')
        ->execute([$userId, $productId, $token, $hash, $status, $expiry, !empty($item['autoRenewingPlan']['autoRenewEnabled']) ? 1 : 0]);

    // Google refunds purchases that are not acknowledged within three days.
    if (($data['acknowledgementState'] ?? '') === 'ACKNOWLEDGEMENT_STATE_PENDING' && in_array($status, ['active', 'grace'], true)) {
        $ack = $http('POST', $base . 'subscriptions/' . rawurlencode($productId) . '/tokens/' . rawurlencode($token) . ':acknowledge',
            array_merge($auth, ['Content-Type: application/json']), '{}');
        if ($ack['status'] !== 200 && $ack['status'] !== 204) log_event('warning', 'plus_acknowledge_failed', ['status' => $ack['status']]);
    }
    return plus_status($pdo, $userId);
}

/** OAuth access token for the Play Developer API from the service-account key (RS256 JWT bearer grant). */
function plus_google_access_token(callable $http): string
{
    static $token = null, $expires = 0;
    if ($token !== null && time() < $expires - 60) return $token;
    if (!plus_configured()) throw new LogicException('Google Play verification is not configured.');
    $key = json_decode((string) file_get_contents((string) getenv('NANNYAPP_PLAY_SERVICE_ACCOUNT_FILE')), true);
    $b64 = fn(string $s) => rtrim(strtr(base64_encode($s), '+/', '-_'), '=');
    $now = time();
    $unsigned = $b64(json_encode(['alg' => 'RS256', 'typ' => 'JWT'])) . '.' . $b64(json_encode([
        'iss' => $key['client_email'], 'scope' => 'https://www.googleapis.com/auth/androidpublisher',
        'aud' => $key['token_uri'] ?? 'https://oauth2.googleapis.com/token', 'iat' => $now, 'exp' => $now + 3600,
    ]));
    if (!openssl_sign($unsigned, $signature, (string) $key['private_key'], OPENSSL_ALGO_SHA256)) throw new RuntimeException('Could not sign the Google token request');
    $reply = $http('POST', $key['token_uri'] ?? 'https://oauth2.googleapis.com/token', ['Content-Type: application/x-www-form-urlencoded'],
        http_build_query(['grant_type' => 'urn:ietf:params:oauth:grant-type:jwt-bearer', 'assertion' => $unsigned . '.' . $b64($signature)]));
    $data = json_decode($reply['body'], true);
    if ($reply['status'] !== 200 || empty($data['access_token'])) throw new RuntimeException('Google token request failed with HTTP ' . $reply['status']);
    $token = (string) $data['access_token'];
    $expires = $now + (int) ($data['expires_in'] ?? 3600);
    return $token;
}

function plus_http(string $method, string $url, array $headers, ?string $body): array
{
    $curl = curl_init($url);
    curl_setopt_array($curl, [CURLOPT_RETURNTRANSFER => true, CURLOPT_CUSTOMREQUEST => $method, CURLOPT_HTTPHEADER => $headers,
        CURLOPT_TIMEOUT => 15, CURLOPT_CONNECTTIMEOUT => 5]);
    if ($body !== null) curl_setopt($curl, CURLOPT_POSTFIELDS, $body);
    $response = curl_exec($curl);
    $status = (int) curl_getinfo($curl, CURLINFO_RESPONSE_CODE);
    curl_close($curl);
    return ['status' => $status, 'body' => is_string($response) ? $response : ''];
}

/** Records a parent's visit to a nanny profile, once per day, for the nanny's "who viewed me" list. */
function plus_record_profile_view(PDO $pdo, int $nannyId, int $viewerId): void
{
    $pdo->prepare('INSERT IGNORE INTO profile_views (nanny_id, viewer_id, view_date, viewed_at) VALUES (?, ?, CURDATE(), NOW())')
        ->execute([$nannyId, $viewerId]);
}

/** Bookings made by a Plus parent go to the top of the nanny's pending requests. */
function plus_mark_priority(PDO $pdo, int $bookingId, int $parentId): void
{
    if (plus_active($pdo, $parentId)) {
        $pdo->prepare('UPDATE bookings SET is_priority = 1 WHERE id = ? AND parent_id = ?')->execute([$bookingId, $parentId]);
    }
}
