<?php
/**
 * Container document-root entry point.
 *
 * Apache/Vercel can discover this file directly, while the shared front
 * controller keeps routing web pages and /api/* endpoints in one place.
 */

declare(strict_types=1);

require __DIR__ . '/api/index.php';
