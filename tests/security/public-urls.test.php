<?php
declare(strict_types=1);
require __DIR__ . '/../../NannyApp.Shared/config/public_urls.php';

function expect_url(string $expected): void {
    $actual = nannyapp_public_base_url();
    if ($actual !== $expected) throw new RuntimeException("Expected $expected, received $actual");
}

// The proxy talks HTTP to Apache; public photos must still use HTTPS.
$_SERVER['HTTP_HOST'] = 'internal-container';
$_SERVER['HTTPS'] = 'off';
putenv('NANNYAPP_BASE_URL=https://nannyapp-two.vercel.app');
expect_url('https://nannyapp-two.vercel.app/');
putenv('NANNYAPP_BASE_URL=https://example.test/nannyapp/');
expect_url('https://example.test/nannyapp/');
putenv('NANNYAPP_BASE_URL');
putenv('VERCEL_URL=preview.example.test');
expect_url('https://preview.example.test/');
putenv('VERCEL_URL');
$_SERVER['HTTP_HOST'] = '127.0.0.1:8088';
putenv('NANNYAPP_BASE_URL=/');
expect_url('http://127.0.0.1:8088/');
putenv('NANNYAPP_BASE_URL=/nannyapp');
expect_url('http://127.0.0.1:8088/nannyapp/');
echo "PASS canonical public URLs behind proxies and in local subdirectories\n";
