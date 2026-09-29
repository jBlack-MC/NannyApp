<?php
/**
 * Upload storage abstraction shared by every Nanny-App client.
 *
 * Why this exists: storing every upload as flat files in one folder doesn't
 * survive going into production — a single directory with tens of thousands
 * of files gets slow to read/back up, and most cloud hosts (Heroku/Render/
 * App Service/containers) wipe local disk on every deploy or don't share it
 * across instances at all. This file provides one small API so the rest of
 * the app doesn't care which of the two is active:
 *
 *   - "local" (default) — files on disk under SHARED_STORAGE_DIR, sharded
 *     into <subdir>/<year>/<month>/ folders so no single folder grows
 *     unbounded. Fine for a single-server / development deployment.
 *   - "s3" — any S3-compatible object storage (AWS S3, Cloudflare R2,
 *     DigitalOcean Spaces, MinIO, ...). Files never touch local disk at all,
 *     so it works across any number of stateless app instances. Turn it on
 *     in production by setting NANNYAPP_STORAGE_DRIVER=s3 plus the
 *     NANNYAPP_S3_* environment variables below — no code changes needed.
 *
 * Secrets are read from environment variables (never hard-code them in the
 * repo); the constants below just capture whatever was set at boot.
 */

declare(strict_types=1);

if (!defined('STORAGE_DRIVER')) define('STORAGE_DRIVER', getenv('NANNYAPP_STORAGE_DRIVER') ?: 'local');

if (!defined('S3_BUCKET'))          define('S3_BUCKET', getenv('NANNYAPP_S3_BUCKET') ?: '');
if (!defined('S3_REGION'))          define('S3_REGION', getenv('NANNYAPP_S3_REGION') ?: 'us-east-1');
if (!defined('S3_ACCESS_KEY'))      define('S3_ACCESS_KEY', getenv('NANNYAPP_S3_ACCESS_KEY') ?: '');
if (!defined('S3_SECRET_KEY'))      define('S3_SECRET_KEY', getenv('NANNYAPP_S3_SECRET_KEY') ?: '');
// Custom endpoint for S3-compatible providers (R2/Spaces/MinIO). Leave blank for real AWS S3.
if (!defined('S3_ENDPOINT'))        define('S3_ENDPOINT', rtrim(getenv('NANNYAPP_S3_ENDPOINT') ?: '', '/'));
// Set this if the bucket/CDN serves objects publicly, to skip presigning every URL.
if (!defined('S3_PUBLIC_BASE_URL')) define('S3_PUBLIC_BASE_URL', rtrim(getenv('NANNYAPP_S3_PUBLIC_BASE_URL') ?: '', '/'));

/** New sharded relative path for a fresh upload, e.g. "uploads/nannies/2026/09/ab12cd34.jpg". */
function storage_new_path(string $subdir, string $ext): string
{
    return trim($subdir, '/\\') . '/' . date('Y/m') . '/' . bin2hex(random_bytes(8)) . '.' . $ext;
}

function storage_mime_for(string $path): string
{
    $ext = strtolower(pathinfo($path, PATHINFO_EXTENSION));
    return [
        'jpg' => 'image/jpeg', 'jpeg' => 'image/jpeg', 'png' => 'image/png',
        'webp' => 'image/webp', 'gif' => 'image/gif', 'pdf' => 'application/pdf',
    ][$ext] ?? 'application/octet-stream';
}

/** Store a just-uploaded temp file ($_FILES[...]['tmp_name']) at $relativePath. */
function storage_safe_path(string $path): bool
{
    return $path !== '' && !str_contains($path, "\0") && !str_contains($path, '\\')
        && !str_starts_with($path,'/') && !preg_match('~(^|/)\.\.?(/|$)|:~',$path);
}

function storage_local_target(string $path): string|false
{
    if (!storage_safe_path($path)) return false;
    $root=realpath(SHARED_STORAGE_DIR);
    if ($root===false) return false;
    $current=$root;
    foreach (explode('/',$path) as $part) {
        if ($part==='') return false;
        $current.='/'.$part;
        if (is_link($current)) return false;
    }
    return $current;
}

/** Caller holds the storage lock, covering both quota calculation and the move. */
function storage_has_capacity(int $incoming): bool
{
    $limit=filter_var(getenv('NANNYAPP_STORAGE_QUOTA_BYTES') ?: '104857600',FILTER_VALIDATE_INT);
    $reserve=filter_var(getenv('NANNYAPP_STORAGE_RESERVE_BYTES') ?: '20971520',FILTER_VALIDATE_INT);
    if ($limit===false || $reserve===false || $limit<1 || $reserve<0 || $incoming<0) return false;
    $used=0;
    try {
        $files=new RecursiveIteratorIterator(new RecursiveDirectoryIterator(SHARED_STORAGE_DIR,FilesystemIterator::SKIP_DOTS));
        foreach ($files as $file) {
            if ($file->isLink()) return false;
            if ($file->isFile()) $used+=$file->getSize();
            if ($used+$incoming>$limit) return false;
        }
        $free=disk_free_space(SHARED_STORAGE_DIR);
        return $used+$incoming<=$limit && $free!==false && $free-$incoming>=$reserve;
    } catch (Throwable $e) { return false; }
}

function storage_store_upload(string $uploadedTmpPath, string $relativePath): bool
{
    if (!storage_safe_path($relativePath) || !is_uploaded_file($uploadedTmpPath)) return false;
    if (STORAGE_DRIVER==='s3') {
        $body=@file_get_contents($uploadedTmpPath);
        return $body!==false && s3_put_object($relativePath,$body,storage_mime_for($relativePath));
    }
    if (!is_dir(SHARED_STORAGE_DIR) && !@mkdir(SHARED_STORAGE_DIR,0700,true)) return false;
    $full=storage_local_target($relativePath);
    if ($full===false || file_exists($full)) return false;
    $lock=@fopen(SHARED_STORAGE_DIR.'/.quota.lock','c');
    if (!$lock) return false;
    try {
        if (!flock($lock,LOCK_EX)) return false;
        $size=filesize($uploadedTmpPath);
        if ($size===false || !storage_has_capacity($size)) return false;
        $dir=dirname($full);
        if (!is_dir($dir) && !@mkdir($dir,0700,true) && !is_dir($dir)) return false;
        if (!move_uploaded_file($uploadedTmpPath,$full)) return false;
        @chmod($full,0600);
        return true;
    } finally { flock($lock,LOCK_UN);fclose($lock); }
}

/** Idempotent delete: missing objects count as success; failures stay in the deletion queue. */
function storage_delete(string $relativePath): bool
{
    if (!storage_safe_path($relativePath)) return false;
    if (STORAGE_DRIVER==='s3') return s3_delete_object($relativePath);
    $full=storage_local_target($relativePath);
    return $full!==false && (!file_exists($full) || (is_file($full) && @unlink($full)));
}

/**
 * Build a browser-facing URL for a stored path. $localMediaUrl builds the
 * caller's own media.php URL (each client streams local-driver files itself
 * since the shared folder sits outside every client's webroot).
 */
function storage_url(string $relativePath, callable $localMediaUrl): string
{
    return $localMediaUrl($relativePath);
}

// ------------------------------------------------------------------------
//  Minimal AWS SigV4 client (no SDK/Composer dependency required).
//  Works against real AWS S3 and any S3-compatible endpoint.
// ------------------------------------------------------------------------

function s3_endpoint(): string
{
    return S3_ENDPOINT !== '' ? S3_ENDPOINT : ('https://s3.' . S3_REGION . '.amazonaws.com');
}

function s3_host(): string
{
    return (string) parse_url(s3_endpoint(), PHP_URL_HOST);
}

function s3_encode_key(string $key): string
{
    return '/' . implode('/', array_map('rawurlencode', explode('/', $key)));
}

function s3_signing_key(string $dateStamp): string
{
    $kDate    = hash_hmac('sha256', $dateStamp, 'AWS4' . S3_SECRET_KEY, true);
    $kRegion  = hash_hmac('sha256', S3_REGION, $kDate, true);
    $kService = hash_hmac('sha256', 's3', $kRegion, true);
    return hash_hmac('sha256', 'aws4_request', $kService, true);
}

function s3_put_object(string $key, string $body, string $contentType): bool
{
    $host        = s3_host();
    $amzDate     = gmdate('Ymd\THis\Z');
    $dateStamp   = gmdate('Ymd');
    $payloadHash = hash('sha256', $body);
    $canonicalUri = '/' . S3_BUCKET . s3_encode_key($key);

    $canonicalHeaders = "content-type:$contentType\nhost:$host\nx-amz-content-sha256:$payloadHash\nx-amz-date:$amzDate\n";
    $signedHeaders    = 'content-type;host;x-amz-content-sha256;x-amz-date';
    $canonicalRequest = "PUT\n$canonicalUri\n\n$canonicalHeaders\n$signedHeaders\n$payloadHash";

    $credentialScope = "$dateStamp/" . S3_REGION . "/s3/aws4_request";
    $stringToSign    = "AWS4-HMAC-SHA256\n$amzDate\n$credentialScope\n" . hash('sha256', $canonicalRequest);
    $signature       = hash_hmac('sha256', $stringToSign, s3_signing_key($dateStamp));

    $authorization = "AWS4-HMAC-SHA256 Credential=" . S3_ACCESS_KEY . "/$credentialScope, "
        . "SignedHeaders=$signedHeaders, Signature=$signature";

    $ch = curl_init(s3_endpoint() . $canonicalUri);
    curl_setopt_array($ch, [
        CURLOPT_CUSTOMREQUEST => 'PUT',
        CURLOPT_POSTFIELDS    => $body,
        CURLOPT_HTTPHEADER    => [
            "Content-Type: $contentType",
            "X-Amz-Content-Sha256: $payloadHash",
            "X-Amz-Date: $amzDate",
            "Authorization: $authorization",
        ],
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_TIMEOUT        => 30,
    ]);
    curl_exec($ch);
    $status = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $error  = curl_error($ch);
    curl_close($ch);

    if ($error !== '') {
        error_log('S3 upload failed: ' . $error);
    }
    return $status >= 200 && $status < 300;
}

function s3_delete_object(string $key): bool
{
    $host      = s3_host();
    $amzDate   = gmdate('Ymd\THis\Z');
    $dateStamp = gmdate('Ymd');
    $payloadHash = hash('sha256', '');
    $canonicalUri = '/' . S3_BUCKET . s3_encode_key($key);

    $canonicalHeaders = "host:$host\nx-amz-content-sha256:$payloadHash\nx-amz-date:$amzDate\n";
    $signedHeaders    = 'host;x-amz-content-sha256;x-amz-date';
    $canonicalRequest = "DELETE\n$canonicalUri\n\n$canonicalHeaders\n$signedHeaders\n$payloadHash";

    $credentialScope = "$dateStamp/" . S3_REGION . "/s3/aws4_request";
    $stringToSign    = "AWS4-HMAC-SHA256\n$amzDate\n$credentialScope\n" . hash('sha256', $canonicalRequest);
    $signature       = hash_hmac('sha256', $stringToSign, s3_signing_key($dateStamp));

    $authorization = "AWS4-HMAC-SHA256 Credential=" . S3_ACCESS_KEY . "/$credentialScope, "
        . "SignedHeaders=$signedHeaders, Signature=$signature";

    $ch = curl_init(s3_endpoint() . $canonicalUri);
    curl_setopt_array($ch, [
        CURLOPT_CUSTOMREQUEST => 'DELETE',
        CURLOPT_HTTPHEADER    => [
            "X-Amz-Content-Sha256: $payloadHash",
            "X-Amz-Date: $amzDate",
            "Authorization: $authorization",
        ],
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_TIMEOUT        => 30,
    ]);
    curl_exec($ch);
    $status = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    return $status >= 200 && $status < 300;
}

/** Presigned GET URL for a private bucket (no S3_PUBLIC_BASE_URL configured). */
function s3_presigned_url(string $key, int $ttlSeconds = 900): string
{
    $host      = s3_host();
    $amzDate   = gmdate('Ymd\THis\Z');
    $dateStamp = gmdate('Ymd');
    $credentialScope = "$dateStamp/" . S3_REGION . "/s3/aws4_request";
    $canonicalUri    = '/' . S3_BUCKET . s3_encode_key($key);

    $queryParams = [
        'X-Amz-Algorithm'     => 'AWS4-HMAC-SHA256',
        'X-Amz-Credential'    => S3_ACCESS_KEY . '/' . $credentialScope,
        'X-Amz-Date'          => $amzDate,
        'X-Amz-Expires'       => (string) $ttlSeconds,
        'X-Amz-SignedHeaders' => 'host',
    ];
    ksort($queryParams);
    $canonicalQuery = http_build_query($queryParams, '', '&', PHP_QUERY_RFC3986);

    $canonicalRequest = "GET\n$canonicalUri\n$canonicalQuery\nhost:$host\n\nhost\nUNSIGNED-PAYLOAD";
    $stringToSign     = "AWS4-HMAC-SHA256\n$amzDate\n$credentialScope\n" . hash('sha256', $canonicalRequest);
    $signature        = hash_hmac('sha256', $stringToSign, s3_signing_key($dateStamp));

    return s3_endpoint() . $canonicalUri . '?' . $canonicalQuery . '&X-Amz-Signature=' . $signature;
}
