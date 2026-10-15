<?php
/** GET /api/content/holidays.php?year=2026 — South African public holidays (no sign-in needed). */
require_once __DIR__ . '/../_bootstrap.php';
require_once __DIR__ . '/../../../../NannyApp.Shared/config/holidays.php';

$year = (int) ($_GET['year'] ?? date('Y'));
$current = (int) date('Y');
if ($year < $current - 1 || $year > $current + 2) json_error('Choose a year close to now.', 400);

try {
    json_response(true, public_holidays(db(), $year));
} catch (Throwable $e) {
    // Holidays are a convenience; a failure here must not stop anyone booking.
    log_exception($e, 'holidays_failed');
    json_response(true, []);
}
