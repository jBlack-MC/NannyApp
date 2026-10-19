<?php
/**
 * Vercel entry point for the existing Nanny-App PHP web and API trees.
 *
 * Vercel invokes one PHP function, while this repository has separate web
 * pages and Android API endpoints. Resolve only known application paths; do
 * not turn this into an arbitrary filesystem include.
 */

declare(strict_types=1);

$requestPath = parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH) ?: '/';
$requestPath = rawurldecode($requestPath);
$requestPath = '/' . ltrim($requestPath, '/');

$webRoot = dirname(__DIR__) . '/NannyApp.Web';
$apiRoot = dirname(__DIR__) . '/NannyApp.Mobile/NannyApp/api';

if (str_starts_with($requestPath, '/api/')) {
    $relative = ltrim(substr($requestPath, 5), '/');
    $target = $apiRoot . '/' . $relative;
} else {
    $relative = ltrim($requestPath, '/');
    $target = $webRoot . '/' . ($relative === '' ? 'index.php' : $relative);
}

$realTarget = realpath($target);
$allowedRoot = str_starts_with($target, $apiRoot . '/') ? $apiRoot : $webRoot;
$realRoot = realpath($allowedRoot);

if ($realTarget === false || $realRoot === false || !str_starts_with($realTarget, $realRoot . DIRECTORY_SEPARATOR)) {
    http_response_code(404);
    require $webRoot . '/404.php';
    exit;
}

if (pathinfo($realTarget, PATHINFO_EXTENSION) !== 'php') {
    http_response_code(404);
    require $webRoot . '/404.php';
    exit;
}

require $realTarget;
