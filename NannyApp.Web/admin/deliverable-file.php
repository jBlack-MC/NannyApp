<?php
require_once __DIR__ . '/../config/config.php';
require_role('admin');

$files = [
    'product-requirements' => ['html' => '01-product-requirements.html', 'pdf' => '01-product-requirements.pdf'],
    'technical-requirements' => ['html' => '02-technical-requirements.html', 'pdf' => '02-technical-requirements.pdf'],
    'app-flow' => ['html' => '03-app-flow.html', 'pdf' => '03-app-flow.pdf'],
    'design-brief' => ['html' => '04-design-brief.html', 'pdf' => '04-design-brief.pdf'],
    'backend-schema' => ['html' => '05-backend-schema.html', 'pdf' => '05-backend-schema.pdf'],
    'implementation-plan' => ['html' => '06-implementation-plan.html', 'pdf' => '06-implementation-plan.pdf'],
    'ci-cd-and-release' => ['html' => '07-ci-cd-and-release.html', 'pdf' => '07-ci-cd-and-release.pdf'],
];
$slug = (string) ($_GET['slug'] ?? '');
$format = (string) ($_GET['format'] ?? '');
if ($format === 'asset') {
    $asset = (string) ($_GET['path'] ?? '');
    if (!str_starts_with($asset, 'assets/') || str_contains($asset, '..')) {
        http_response_code(404);
        exit('Asset not found.');
    }
    $assetRoot = realpath(__DIR__ . '/../../docs/deliverables');
    $assetPath = $assetRoot === false ? false : realpath($assetRoot . DIRECTORY_SEPARATOR . $asset);
    if ($assetPath === false || !str_starts_with($assetPath, $assetRoot . DIRECTORY_SEPARATOR) || !is_file($assetPath)) {
        http_response_code(404);
        exit('Asset not found.');
    }
    $mime = mime_content_type($assetPath) ?: 'application/octet-stream';
    header('Content-Type: ' . $mime);
    header('Cache-Control: private, max-age=3600');
    readfile($assetPath);
    exit;
}
if (!isset($files[$slug]) || !in_array($format, ['html', 'pdf'], true)) {
    http_response_code(404);
    exit('Document not found.');
}

$root = realpath(__DIR__ . '/../../docs/deliverables');
$path = $root === false ? false : realpath($root . DIRECTORY_SEPARATOR . $files[$slug][$format]);
if ($path === false || !str_starts_with($path, $root . DIRECTORY_SEPARATOR) || !is_file($path)) {
    http_response_code(404);
    exit('Document not found.');
}

if ($format === 'pdf') {
    header('Content-Type: application/pdf');
    header('Content-Disposition: attachment; filename="' . basename($path) . '"');
    header('X-Content-Type-Options: nosniff');
    readfile($path);
    exit;
}

$html = file_get_contents($path);
if ($html === false) {
    http_response_code(404);
    exit('Document not found.');
}

// Keep the original HTML documents reusable while routing their relative
// stylesheet/script links through the same authenticated endpoint.
$assetBase = url('admin/deliverable-file.php?slug=' . rawurlencode($slug) . '&format=asset&path=');
$html = str_replace(['href="assets/', 'src="assets/'], ['href="' . $assetBase, 'src="' . $assetBase], $html);
foreach ($files as $targetSlug => $targetFiles) {
    $html = str_replace(
        'href="' . $targetFiles['html'] . '"',
        'href="' . url('admin/deliverable-file.php?slug=' . rawurlencode($targetSlug) . '&format=html') . '"',
        $html
    );
}
header('Content-Type: text/html; charset=utf-8');
header('Content-Security-Policy: default-src \'self\'; style-src \'self\' \'unsafe-inline\'; script-src \'self\'; img-src \'self\' data:; base-uri \'none\'; frame-ancestors \'self\'');
echo $html;
