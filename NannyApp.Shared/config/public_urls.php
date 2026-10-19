<?php
declare(strict_types=1);

/** Canonical deployment URL, including the optional XAMPP subdirectory. */
function nannyapp_public_base_url(): string
{
    $configured = trim((string) (getenv('NANNYAPP_BASE_URL') ?: ''));
    if (preg_match('~^https?://~i', $configured)) {
        return rtrim($configured, '/') . '/';
    }
    $vercelHost = trim((string) (getenv('VERCEL_URL') ?: ''));
    if ($vercelHost !== '') return 'https://' . $vercelHost . '/';

    // Local/self-hosted fallback. Production should set NANNYAPP_BASE_URL.
    $https = !empty($_SERVER['HTTPS']) && strtolower((string) $_SERVER['HTTPS']) !== 'off';
    $scheme = $https ? 'https' : 'http';
    $host = $_SERVER['HTTP_HOST'] ?? 'localhost';
    $path = $configured !== '' ? $configured : '/nannyapp';
    return $scheme . '://' . $host . '/' . trim($path, '/') . (trim($path, '/') !== '' ? '/' : '');
}
