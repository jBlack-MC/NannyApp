<?php
declare(strict_types=1);

/** Only the worker calls the provider. A fixed HTTPS origin prevents credential forwarding. */
function resend_http(string $payload, string $key, string $secret): array
{
    $body = '';
    $retryAfter = '';
    $ch = curl_init('https://api.resend.com/emails');
    if ($ch === false) throw new RuntimeException('mail_transport_unavailable');
    try {
        curl_setopt_array($ch, [
            CURLOPT_POST => true,
            CURLOPT_POSTFIELDS => $payload,
            CURLOPT_HTTPHEADER => ['Content-Type: application/json', 'Accept: application/json', 'Authorization: Bearer ' . $secret, 'Idempotency-Key: ' . $key],
            CURLOPT_CONNECTTIMEOUT => 3,
            CURLOPT_TIMEOUT => 10,
            CURLOPT_FOLLOWLOCATION => false,
            CURLOPT_PROTOCOLS => CURLPROTO_HTTPS,
            CURLOPT_SSL_VERIFYPEER => true,
            CURLOPT_SSL_VERIFYHOST => 2,
            CURLOPT_WRITEFUNCTION => static function ($handle, string $chunk) use (&$body): int {
                if (strlen($body) + strlen($chunk) > 65536) return 0;
                $body .= $chunk;
                return strlen($chunk);
            },
            CURLOPT_HEADERFUNCTION => static function ($handle, string $line) use (&$retryAfter): int {
                if (stripos($line, 'Retry-After:') === 0) $retryAfter = trim(substr($line, 12));
                return strlen($line);
            },
        ]);
        $ok = curl_exec($ch);
        return ['status' => $ok === false ? 0 : (int) curl_getinfo($ch, CURLINFO_HTTP_CODE), 'body' => $body, 'retry_after' => $retryAfter];
    } finally {
        curl_close($ch);
    }
}

/** Injectable HTTP boundary permits failure tests without sending email or weakening TLS. */
function resend_deliver(array $job, ?callable $http = null): array
{
    $secret = getenv('NANNYAPP_RESEND_API_KEY') ?: '';
    if ($secret === '' || preg_match('/[\r\n]/', $secret)) {
        return ['state' => 'retry', 'code' => 'configuration', 'delay' => 300];
    }
    try {
        $reply = ($http ?? 'resend_http')($job['payload'], 'nanny-email/' . $job['id'], $secret);
    } catch (Throwable) {
        return ['state' => 'retry', 'code' => 'transport', 'delay' => 0];
    }
    $status = $reply['status'] ?? 0;
    $raw = $reply['body'] ?? '';
    $decoded = is_string($raw) && strlen($raw) <= 65536 ? json_decode($raw, true) : null;
    if ($status === 200 || $status === 201) {
        $id = is_array($decoded) ? ($decoded['id'] ?? null) : null;
        if (is_string($id) && preg_match('/^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$/i', $id)) {
            return ['state' => 'sent', 'code' => 'accepted', 'provider_id' => $id, 'delay' => 0];
        }
        return ['state' => 'retry', 'code' => 'invalid_response', 'delay' => 0];
    }
    // 409 can mean an identical request is already being processed; reuse its key.
    $retry = $status === 0 || $status === 408 || $status === 409 || $status === 429 || (is_int($status) && $status >= 500);
    $header = $reply['retry_after'] ?? '';
    $delay = is_string($header) && ctype_digit($header) ? min(86400, (int) $header) : 0;
    return ['state' => $retry ? 'retry' : 'failed', 'code' => is_int($status) ? 'http_' . $status : 'invalid_status', 'delay' => $delay];
}
